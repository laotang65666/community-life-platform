package com.laotang.core.marketing.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.laotang.common.constant.RedisKey;
import com.laotang.common.exception.BusinessException;
import com.laotang.common.exception.ErrorCode;
import com.laotang.core.idempotency.entity.IdempotencyRecord;
import com.laotang.core.idempotency.service.IdempotencyService;
import com.laotang.core.marketing.entity.Voucher;
import com.laotang.core.marketing.entity.VoucherOrder;
import com.laotang.core.marketing.kafka.SeckillOrderMessage;
import com.laotang.core.marketing.mapper.VoucherMapper;
import com.laotang.core.marketing.mapper.VoucherOrderMapper;
import com.laotang.core.marketing.service.ISeckillService;
import com.laotang.core.outbox.service.OutboxService;
import com.laotang.framework.redis.ratelimit.RedisRateLimiter;
import com.laotang.id.SnowflakeIdGenerator;
import com.laotang.parameter.marketing.response.SeckillOrderResponse;
import com.laotang.parameter.marketing.response.SeckillTokenResponse;
import com.laotang.parameter.marketing.response.VoucherOrderResponse;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 秒杀服务实现。
 *
 * <p>核心链路:令牌 → Lua 原子扣减 → 写 Outbox → Kafka 异步落库。</p>
 */
@Service
public class SeckillServiceImpl implements ISeckillService {

    private static final String SECKILL_TOPIC = "clp-seckill-order";
    private static final String BIZ_TYPE = "SECKILL_ORDER";

    private static final Duration TOKEN_TTL = Duration.ofSeconds(60);
    private static final int TOKEN_RATE_LIMIT = 5;
    private static final Duration TOKEN_RATE_WINDOW = Duration.ofSeconds(1);

    private final VoucherMapper voucherMapper;
    private final VoucherOrderMapper voucherOrderMapper;
    private final StringRedisTemplate redis;
    private final RedisRateLimiter rateLimiter;
    private final IdempotencyService idempotencyService;
    private final OutboxService outboxService;
    private final SnowflakeIdGenerator snowflakeIdGenerator;
    private final ObjectMapper objectMapper;
    private final DefaultRedisScript<Long> seckillScript;

    public SeckillServiceImpl(VoucherMapper voucherMapper,
                              VoucherOrderMapper voucherOrderMapper,
                              StringRedisTemplate redis,
                              RedisRateLimiter rateLimiter,
                              IdempotencyService idempotencyService,
                              OutboxService outboxService,
                              SnowflakeIdGenerator snowflakeIdGenerator,
                              ObjectMapper objectMapper,
                              @Qualifier("seckillScript") DefaultRedisScript<Long> seckillScript) {
        this.voucherMapper = voucherMapper;
        this.voucherOrderMapper = voucherOrderMapper;
        this.redis = redis;
        this.rateLimiter = rateLimiter;
        this.idempotencyService = idempotencyService;
        this.outboxService = outboxService;
        this.snowflakeIdGenerator = snowflakeIdGenerator;
        this.objectMapper = objectMapper;
        this.seckillScript = seckillScript;
    }

    @Override
    public SeckillTokenResponse createToken(Long userId, Long voucherId) {
        Voucher voucher = requireVoucher(voucherId);
        checkTime(voucher);

        if (!rateLimiter.tryAcquire(
                RedisKey.seckillTokenLimit(userId, voucherId), TOKEN_RATE_LIMIT, TOKEN_RATE_WINDOW)) {
            throw new BusinessException(ErrorCode.SECKILL_RATE_LIMITED);
        }

        Boolean bought = redis.opsForSet()
                .isMember(RedisKey.seckillUsers(voucherId), String.valueOf(userId));
        if (Boolean.TRUE.equals(bought)) {
            throw new BusinessException(ErrorCode.SECKILL_DUPLICATE);
        }

        String token = UUID.randomUUID().toString().replace("-", "");
        redis.opsForValue().set(RedisKey.seckillToken(token), String.valueOf(userId), TOKEN_TTL);
        return new SeckillTokenResponse(token, TOKEN_TTL.toSeconds());
    }

    @Override
    public SeckillOrderResponse seckill(Long userId, String idempotencyKey, Long voucherId, String token) {
        if (!StringUtils.hasText(idempotencyKey)) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "缺少 Idempotency-Key");
        }

        IdempotencyRecord acquired = idempotencyService.tryAcquire(userId, idempotencyKey, BIZ_TYPE);
        if (acquired == null) {
            IdempotencyRecord existing = idempotencyService.find(userId, idempotencyKey);
            if (existing == null) {
                throw new BusinessException(ErrorCode.REQUEST_PROCESSING);
            }
            if (IdempotencyService.SUCCESS.equals(existing.getStatus())) {
                return new SeckillOrderResponse(existing.getBizId(), "PROCESSING");
            }
            if (IdempotencyService.PROCESSING.equals(existing.getStatus())) {
                throw new BusinessException(ErrorCode.REQUEST_PROCESSING);
            }
            // FAILED:释放后重试一次
            idempotencyService.release(existing.getId());
            acquired = idempotencyService.tryAcquire(userId, idempotencyKey, BIZ_TYPE);
            if (acquired == null) {
                throw new BusinessException(ErrorCode.REQUEST_PROCESSING);
            }
        }

        Voucher voucher = requireVoucher(voucherId);
        checkTime(voucher);

        Long result;
        try {
            result = redis.execute(
                    seckillScript,
                    List.of(
                            RedisKey.seckillStock(voucherId),
                            RedisKey.seckillUsers(voucherId),
                            RedisKey.seckillToken(token)),
                    String.valueOf(userId));
        } catch (Exception e) {
            idempotencyService.release(acquired.getId());
            throw new BusinessException(ErrorCode.SYSTEM_ERROR);
        }

        long code = result == null ? -1 : result;
        if (code != 0) {
            idempotencyService.release(acquired.getId());
            throw mapLuaResult(code);
        }

        String orderNo = String.valueOf(snowflakeIdGenerator.nextId());
        String messageId = UUID.randomUUID().toString();
        SeckillOrderMessage message = new SeckillOrderMessage(
                messageId, orderNo, userId, voucherId,
                voucher.getShopId(), voucher.getPayValue(), System.currentTimeMillis());

        try {
            String payload = objectMapper.writeValueAsString(message);
            outboxService.enqueue(SECKILL_TOPIC, BIZ_TYPE, orderNo, payload, messageId);
        } catch (Exception e) {
            // 写 Outbox 失败:补偿 Redis,恢复库存和用户标记,允许用户重试
            redis.opsForValue().increment(RedisKey.seckillStock(voucherId));
            redis.opsForSet().remove(RedisKey.seckillUsers(voucherId), String.valueOf(userId));
            idempotencyService.release(acquired.getId());
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "秒杀受理失败,请重试");
        }

        idempotencyService.markSuccess(acquired.getId(), orderNo);
        return new SeckillOrderResponse(orderNo, "PROCESSING");
    }

    @Override
    public VoucherOrderResponse myOrder(Long userId, Long voucherId) {
        VoucherOrder order = voucherOrderMapper.selectOne(new LambdaQueryWrapper<VoucherOrder>()
                .eq(VoucherOrder::getVoucherId, voucherId)
                .eq(VoucherOrder::getUserId, userId));
        return order == null ? null : com.laotang.core.marketing.converter.VoucherConverter.toOrderResponse(order);
    }

    private BusinessException mapLuaResult(long code) {
        return switch ((int) code) {
            case 1 -> new BusinessException(ErrorCode.SECKILL_STOCK_EMPTY);
            case 2 -> new BusinessException(ErrorCode.SECKILL_DUPLICATE);
            case 3 -> new BusinessException(ErrorCode.SECKILL_TOKEN_INVALID);
            default -> new BusinessException(ErrorCode.SYSTEM_ERROR);
        };
    }

    private Voucher requireVoucher(Long voucherId) {
        Voucher voucher = voucherMapper.selectById(voucherId);
        if (voucher == null) {
            throw new BusinessException(ErrorCode.VOUCHER_NOT_FOUND);
        }
        return voucher;
    }

    private void checkTime(Voucher voucher) {
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(voucher.getBeginTime())) {
            throw new BusinessException(ErrorCode.SECKILL_NOT_STARTED);
        }
        if (now.isAfter(voucher.getEndTime())) {
            throw new BusinessException(ErrorCode.SECKILL_ENDED);
        }
    }
}
