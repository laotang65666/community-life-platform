package com.laotang.core.marketing.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.laotang.common.constant.RedisKey;
import com.laotang.common.exception.BusinessException;
import com.laotang.common.exception.ErrorCode;
import com.laotang.core.idempotency.service.RedisIdempotencyService;
import com.laotang.core.marketing.converter.VoucherConverter;
import com.laotang.core.marketing.entity.VoucherOrder;
import com.laotang.core.marketing.kafka.SeckillOrderMessage;
import com.laotang.core.marketing.mapper.VoucherOrderMapper;
import com.laotang.core.marketing.service.ISeckillService;
import com.laotang.core.marketing.support.SeckillVoucherCache;
import com.laotang.core.marketing.support.SeckillVoucherCache.VoucherInfo;
import com.laotang.core.outbox.service.OutboxService;
import com.laotang.framework.redis.ratelimit.RedisRateLimiter;
import com.laotang.id.SnowflakeIdGenerator;
import com.laotang.parameter.marketing.response.SeckillOrderResponse;
import com.laotang.parameter.marketing.response.SeckillTokenResponse;
import com.laotang.parameter.marketing.response.VoucherOrderResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

/**
 * 秒杀服务实现。
 *
 * <p>性能优化后的路径:</p>
 * <pre>
 * 1. Redis SETNX 幂等抢占(不写数据库)
 * 2. Redis Hash 读取券信息(不查数据库)
 * 3. Redis + Lua 原子扣减
 * 4. 只有成功请求才写一次 Outbox
 * </pre>
 */
@Service
public class SeckillServiceImpl implements ISeckillService {

    private static final String SECKILL_TOPIC = "clp-seckill-order";
    private static final String BIZ_TYPE = "SECKILL_ORDER";

    private static final Duration TOKEN_TTL = Duration.ofSeconds(60);
    private static final Duration IDEM_TTL = Duration.ofHours(24);
    private static final int TOKEN_RATE_LIMIT = 5;
    private static final Duration TOKEN_RATE_WINDOW = Duration.ofSeconds(1);

    private final VoucherOrderMapper voucherOrderMapper;
    private final StringRedisTemplate redis;
    private final RedisRateLimiter rateLimiter;
    private final RedisIdempotencyService redisIdempotency;
    private final SeckillVoucherCache voucherCache;
    private final OutboxService outboxService;
    private final SnowflakeIdGenerator snowflakeIdGenerator;
    private final ObjectMapper objectMapper;
    private final DefaultRedisScript<Long> seckillScript;

    public SeckillServiceImpl(VoucherOrderMapper voucherOrderMapper,
                              StringRedisTemplate redis,
                              RedisRateLimiter rateLimiter,
                              RedisIdempotencyService redisIdempotency,
                              SeckillVoucherCache voucherCache,
                              OutboxService outboxService,
                              SnowflakeIdGenerator snowflakeIdGenerator,
                              ObjectMapper objectMapper,
                              @Qualifier("seckillScript") DefaultRedisScript<Long> seckillScript) {
        this.voucherOrderMapper = voucherOrderMapper;
        this.redis = redis;
        this.rateLimiter = rateLimiter;
        this.redisIdempotency = redisIdempotency;
        this.voucherCache = voucherCache;
        this.outboxService = outboxService;
        this.snowflakeIdGenerator = snowflakeIdGenerator;
        this.objectMapper = objectMapper;
        this.seckillScript = seckillScript;
    }

    @Override
    public SeckillTokenResponse createToken(Long userId, Long voucherId) {
        VoucherInfo voucher = voucherCache.get(voucherId);
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

        String idemKey = RedisKey.seckillIdempotency(userId, idempotencyKey);
        if (!redisIdempotency.tryAcquire(idemKey, IDEM_TTL)) {
            String existing = redisIdempotency.get(idemKey);
            if (existing == null) {
                throw new BusinessException(ErrorCode.REQUEST_PROCESSING);
            }
            if (!RedisIdempotencyService.PROCESSING.equals(existing)) {
                return new SeckillOrderResponse(existing, "PROCESSING");
            }
            throw new BusinessException(ErrorCode.REQUEST_PROCESSING);
        }

        VoucherInfo voucher;
        try {
            voucher = voucherCache.get(voucherId);
            checkTime(voucher);
        } catch (RuntimeException e) {
            redisIdempotency.release(idemKey);
            throw e;
        }

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
            redisIdempotency.release(idemKey);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR);
        }

        long code = result == null ? -1 : result;
        if (code != 0) {
            redisIdempotency.release(idemKey);
            throw mapLuaResult(code);
        }

        String orderNo = String.valueOf(snowflakeIdGenerator.nextId());
        String messageId = UUID.randomUUID().toString();
        SeckillOrderMessage message = new SeckillOrderMessage(
                messageId, orderNo, userId, voucherId,
                voucher.shopId(), voucher.payValue(), System.currentTimeMillis());

        try {
            String payload = objectMapper.writeValueAsString(message);
            outboxService.enqueue(SECKILL_TOPIC, BIZ_TYPE, orderNo, payload, messageId);
        } catch (Exception e) {
            // 写 Outbox 失败:补偿 Redis,恢复库存和用户标记
            redis.opsForValue().increment(RedisKey.seckillStock(voucherId));
            redis.opsForSet().remove(RedisKey.seckillUsers(voucherId), String.valueOf(userId));
            redisIdempotency.release(idemKey);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "秒杀受理失败,请重试");
        }

        redisIdempotency.markSuccess(idemKey, orderNo, IDEM_TTL);
        return new SeckillOrderResponse(orderNo, "PROCESSING");
    }

    @Override
    public VoucherOrderResponse myOrder(Long userId, Long voucherId) {
        VoucherOrder order = voucherOrderMapper.selectOne(new LambdaQueryWrapper<VoucherOrder>()
                .eq(VoucherOrder::getVoucherId, voucherId)
                .eq(VoucherOrder::getUserId, userId));
        return order == null ? null : VoucherConverter.toOrderResponse(order);
    }

    private BusinessException mapLuaResult(long code) {
        return switch ((int) code) {
            case 1 -> new BusinessException(ErrorCode.SECKILL_STOCK_EMPTY);
            case 2 -> new BusinessException(ErrorCode.SECKILL_DUPLICATE);
            case 3 -> new BusinessException(ErrorCode.SECKILL_TOKEN_INVALID);
            default -> new BusinessException(ErrorCode.SYSTEM_ERROR);
        };
    }

    private void checkTime(VoucherInfo voucher) {
        long now = System.currentTimeMillis();
        if (now < voucher.beginAt()) {
            throw new BusinessException(ErrorCode.SECKILL_NOT_STARTED);
        }
        if (now > voucher.endAt()) {
            throw new BusinessException(ErrorCode.SECKILL_ENDED);
        }
    }
}
