package com.laotang.core.marketing.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.laotang.common.exception.BusinessException;
import com.laotang.common.exception.ErrorCode;
import com.laotang.core.idempotency.entity.IdempotencyRecord;
import com.laotang.core.idempotency.service.IdempotencyService;
import com.laotang.core.marketing.entity.Voucher;
import com.laotang.core.marketing.mapper.VoucherMapper;
import com.laotang.core.marketing.mapper.VoucherOrderMapper;
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

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SeckillServiceImplTest {

    @Mock
    private VoucherMapper voucherMapper;
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
    private IdempotencyService idempotencyService;
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
                voucherMapper, voucherOrderMapper, redis, rateLimiter,
                idempotencyService, outboxService, snowflakeIdGenerator,
                objectMapper, script);
    }

    private Voucher voucher() {
        Voucher voucher = new Voucher();
        voucher.setId(100L);
        voucher.setShopId(10L);
        voucher.setPayValue(1000L);
        voucher.setBeginTime(LocalDateTime.now().minusHours(1));
        voucher.setEndTime(LocalDateTime.now().plusHours(1));
        return voucher;
    }

    @Test
    @DisplayName("优惠券不存在 -> 7000")
    void createToken_notFound() {
        when(voucherMapper.selectById(100L)).thenReturn(null);

        BusinessException ex = catchThrowableOfType(
                () -> seckillService.createToken(1L, 100L), BusinessException.class);

        assertThat(ex).isNotNull();
        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.VOUCHER_NOT_FOUND);
    }

    @Test
    @DisplayName("秒杀未开始 -> 7001")
    void createToken_notStarted() {
        Voucher voucher = voucher();
        voucher.setBeginTime(LocalDateTime.now().plusHours(1));
        when(voucherMapper.selectById(100L)).thenReturn(voucher);

        BusinessException ex = catchThrowableOfType(
                () -> seckillService.createToken(1L, 100L), BusinessException.class);

        assertThat(ex).isNotNull();
        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.SECKILL_NOT_STARTED);
    }

    @Test
    @DisplayName("触发限流 -> 7006")
    void createToken_rateLimited() {
        when(voucherMapper.selectById(100L)).thenReturn(voucher());
        when(rateLimiter.tryAcquire(anyString(), any(Integer.class), any())).thenReturn(false);

        BusinessException ex = catchThrowableOfType(
                () -> seckillService.createToken(1L, 100L), BusinessException.class);

        assertThat(ex).isNotNull();
        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.SECKILL_RATE_LIMITED);
    }

    @Test
    @DisplayName("已购买过 -> 7004")
    void createToken_alreadyBought() {
        when(voucherMapper.selectById(100L)).thenReturn(voucher());
        when(rateLimiter.tryAcquire(anyString(), any(Integer.class), any())).thenReturn(true);
        when(setOps.isMember(anyString(), anyString())).thenReturn(true);

        BusinessException ex = catchThrowableOfType(
                () -> seckillService.createToken(1L, 100L), BusinessException.class);

        assertThat(ex).isNotNull();
        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.SECKILL_DUPLICATE);
    }

    @Test
    @DisplayName("令牌创建成功,写入 Redis 并返回 60 秒有效期")
    void createToken_success() {
        when(voucherMapper.selectById(100L)).thenReturn(voucher());
        when(rateLimiter.tryAcquire(anyString(), any(Integer.class), any())).thenReturn(true);
        when(setOps.isMember(anyString(), anyString())).thenReturn(false);

        SeckillTokenResponse response = seckillService.createToken(1L, 100L);

        assertThat(response.token()).isNotBlank();
        assertThat(response.expiresIn()).isEqualTo(60);
        verify(valueOps).set(anyString(), anyString(), any(java.time.Duration.class));
    }

    @Test
    @DisplayName("Lua 返回库存不足 -> 7003")
    void seckill_stockEmpty() {
        IdempotencyRecord acquired = new IdempotencyRecord();
        acquired.setId(1L);
        when(idempotencyService.tryAcquire(1L, "key-1", "SECKILL_ORDER")).thenReturn(acquired);
        when(voucherMapper.selectById(100L)).thenReturn(voucher());
        when(redis.execute(any(RedisScript.class), anyList(), any())).thenReturn(1L);

        BusinessException ex = catchThrowableOfType(
                () -> seckillService.seckill(1L, "key-1", 100L, "token"),
                BusinessException.class);

        assertThat(ex).isNotNull();
        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.SECKILL_STOCK_EMPTY);
        verify(idempotencyService).release(1L);
    }

    @Test
    @DisplayName("幂等重放 -> 返回首次订单号,不再执行 Lua")
    void seckill_idempotentReplay() {
        when(idempotencyService.tryAcquire(1L, "key-1", "SECKILL_ORDER")).thenReturn(null);
        IdempotencyRecord existing = new IdempotencyRecord();
        existing.setStatus(IdempotencyService.SUCCESS);
        existing.setBizId("999");
        when(idempotencyService.find(1L, "key-1")).thenReturn(existing);

        SeckillOrderResponse response = seckillService.seckill(1L, "key-1", 100L, "token");

        assertThat(response.orderNo()).isEqualTo("999");
    }

    @Test
    @DisplayName("秒杀成功 -> 写 Outbox 并返回订单号")
    void seckill_success() {
        IdempotencyRecord acquired = new IdempotencyRecord();
        acquired.setId(1L);
        when(idempotencyService.tryAcquire(1L, "key-1", "SECKILL_ORDER")).thenReturn(acquired);
        when(voucherMapper.selectById(100L)).thenReturn(voucher());
        when(redis.execute(any(RedisScript.class), anyList(), any())).thenReturn(0L);
        when(snowflakeIdGenerator.nextId()).thenReturn(123456L);
        when(outboxService.enqueue(anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn("msg-1");

        SeckillOrderResponse response = seckillService.seckill(1L, "key-1", 100L, "token");

        assertThat(response.orderNo()).isEqualTo("123456");
        assertThat(response.status()).isEqualTo("PROCESSING");
        verify(idempotencyService).markSuccess(1L, "123456");
    }
}
