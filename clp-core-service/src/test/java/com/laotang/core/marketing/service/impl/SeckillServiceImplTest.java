package com.laotang.core.marketing.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.laotang.common.exception.BusinessException;
import com.laotang.common.exception.ErrorCode;
import com.laotang.core.idempotency.service.RedisIdempotencyService;
import com.laotang.core.marketing.mapper.VoucherOrderMapper;
import com.laotang.core.marketing.support.SeckillVoucherCache;
import com.laotang.core.marketing.support.SeckillVoucherCache.VoucherInfo;
import com.laotang.core.outbox.service.OutboxService;
import com.laotang.framework.redis.ratelimit.RedisRateLimiter;
import com.laotang.id.SnowflakeIdGenerator;
import com.laotang.parameter.marketing.response.SeckillOrderResponse;
import com.laotang.parameter.marketing.response.SeckillTokenResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;

import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SeckillServiceImplTest {

    @Mock
    private VoucherOrderMapper voucherOrderMapper;
    @Mock
    private StringRedisTemplate redis;
    @Mock
    private ValueOperations<String, String> valueOps;
    @Mock
    private SetOperations<String, String> setOps;
    @Mock
    private RedisRateLimiter rateLimiter;
    @Mock
    private RedisIdempotencyService redisIdempotency;
    @Mock
    private SeckillVoucherCache voucherCache;
    @Mock
    private OutboxService outboxService;
    @Mock
    private SnowflakeIdGenerator snowflakeIdGenerator;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final DefaultRedisScript<Long> script = new DefaultRedisScript<>();

    private SeckillServiceImpl seckillService;

    @BeforeEach
    void setUp() {
        lenient().when(redis.opsForValue()).thenReturn(valueOps);
        lenient().when(redis.opsForSet()).thenReturn(setOps);
        seckillService = new SeckillServiceImpl(
                voucherOrderMapper, redis, rateLimiter, redisIdempotency,
                voucherCache, outboxService, snowflakeIdGenerator, objectMapper, script);
    }

    private VoucherInfo voucherInfo() {
        long now = System.currentTimeMillis();
        return new VoucherInfo(10L, 1000L, now - 3_600_000, now + 3_600_000);
    }

    private String idemKey() {
        return "clp:idem:seckill:1:key-1";
    }

    @Test
    @DisplayName("优惠券不存在 -> 7000")
    void createToken_notFound() {
        when(voucherCache.get(100L)).thenThrow(new BusinessException(ErrorCode.VOUCHER_NOT_FOUND));

        BusinessException ex = catchThrowableOfType(
                () -> seckillService.createToken(1L, 100L), BusinessException.class);

        assertThat(ex).isNotNull();
        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.VOUCHER_NOT_FOUND);
    }

    @Test
    @DisplayName("秒杀未开始 -> 7001")
    void createToken_notStarted() {
        long now = System.currentTimeMillis();
        when(voucherCache.get(100L))
                .thenReturn(new VoucherInfo(10L, 1000L, now + 3_600_000, now + 7_200_000));

        BusinessException ex = catchThrowableOfType(
                () -> seckillService.createToken(1L, 100L), BusinessException.class);

        assertThat(ex).isNotNull();
        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.SECKILL_NOT_STARTED);
    }

    @Test
    @DisplayName("触发限流 -> 7006")
    void createToken_rateLimited() {
        when(voucherCache.get(100L)).thenReturn(voucherInfo());
        when(rateLimiter.tryAcquire(anyString(), eq(5), any(Duration.class))).thenReturn(false);

        BusinessException ex = catchThrowableOfType(
                () -> seckillService.createToken(1L, 100L), BusinessException.class);

        assertThat(ex).isNotNull();
        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.SECKILL_RATE_LIMITED);
    }

    @Test
    @DisplayName("已购买过 -> 7004")
    void createToken_alreadyBought() {
        when(voucherCache.get(100L)).thenReturn(voucherInfo());
        when(rateLimiter.tryAcquire(anyString(), eq(5), any(Duration.class))).thenReturn(true);
        when(setOps.isMember(anyString(), anyString())).thenReturn(true);

        BusinessException ex = catchThrowableOfType(
                () -> seckillService.createToken(1L, 100L), BusinessException.class);

        assertThat(ex).isNotNull();
        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.SECKILL_DUPLICATE);
    }

    @Test
    @DisplayName("令牌创建成功")
    void createToken_success() {
        when(voucherCache.get(100L)).thenReturn(voucherInfo());
        when(rateLimiter.tryAcquire(anyString(), eq(5), any(Duration.class))).thenReturn(true);
        when(setOps.isMember(anyString(), anyString())).thenReturn(false);

        SeckillTokenResponse response = seckillService.createToken(1L, 100L);

        assertThat(response.token()).isNotBlank();
        assertThat(response.expiresIn()).isEqualTo(60);
        verify(valueOps).set(anyString(), anyString(), any(Duration.class));
    }

    @Test
    @DisplayName("Lua 返回库存不足 -> 7003,释放幂等键")
    void seckill_stockEmpty() {
        when(redisIdempotency.tryAcquire(eq(idemKey()), any(Duration.class))).thenReturn(true);
        when(voucherCache.get(100L)).thenReturn(voucherInfo());
        when(redis.execute(any(RedisScript.class), anyList(), any())).thenReturn(1L);

        BusinessException ex = catchThrowableOfType(
                () -> seckillService.seckill(1L, "key-1", 100L, "token"),
                BusinessException.class);

        assertThat(ex).isNotNull();
        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.SECKILL_STOCK_EMPTY);
        verify(redisIdempotency).release(idemKey());
    }

    @Test
    @DisplayName("幂等重放 -> 返回首次订单号")
    void seckill_idempotentReplay() {
        when(redisIdempotency.tryAcquire(eq(idemKey()), any(Duration.class))).thenReturn(false);
        when(redisIdempotency.get(idemKey())).thenReturn("999");

        SeckillOrderResponse response = seckillService.seckill(1L, "key-1", 100L, "token");

        assertThat(response.orderNo()).isEqualTo("999");
    }

    @Test
    @DisplayName("秒杀成功 -> 写 Outbox 并记录幂等结果")
    void seckill_success() {
        when(redisIdempotency.tryAcquire(eq(idemKey()), any(Duration.class))).thenReturn(true);
        when(voucherCache.get(100L)).thenReturn(voucherInfo());
        when(redis.execute(any(RedisScript.class), anyList(), any())).thenReturn(0L);
        when(snowflakeIdGenerator.nextId()).thenReturn(123456L);
        when(outboxService.enqueue(anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn("msg-1");

        SeckillOrderResponse response = seckillService.seckill(1L, "key-1", 100L, "token");

        assertThat(response.orderNo()).isEqualTo("123456");
        assertThat(response.status()).isEqualTo("PROCESSING");
        verify(redisIdempotency).markSuccess(eq(idemKey()), eq("123456"), any(Duration.class));
    }
}
