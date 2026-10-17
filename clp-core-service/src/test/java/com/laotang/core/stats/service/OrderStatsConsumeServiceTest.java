package com.laotang.core.stats.service;

import com.laotang.core.stats.event.OrderEvent;
import com.laotang.core.stats.mapper.OrderEventLogMapper;
import com.laotang.core.stats.mapper.ShopDailyStatsMapper;
import com.laotang.id.SnowflakeIdGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderStatsConsumeServiceTest {

    private static final LocalDateTime CREATED_AT =
            LocalDateTime.of(2026, 10, 17, 10, 0);

    @Mock
    private OrderEventLogMapper eventLogMapper;
    @Mock
    private ShopDailyStatsMapper statsMapper;
    @Mock
    private SnowflakeIdGenerator idGenerator;

    private OrderStatsConsumeService service;

    @BeforeEach
    void setUp() {
        service = new OrderStatsConsumeService(eventLogMapper, statsMapper, idGenerator);
        lenient().when(idGenerator.nextId()).thenReturn(1L);
    }

    private OrderEvent event(String type, Long amount) {
        return new OrderEvent(
                "evt-1", type, 500L, "ON-500", 1L, 10L,
                amount, CREATED_AT, LocalDateTime.now());
    }

    @Test
    @DisplayName("下单事件累加订单数")
    void consume_created() {
        when(eventLogMapper.insertIgnore(anyLong(), eq("evt-1"), eq("ORDER_CREATED"),
                eq(500L), eq(10L), any(LocalDate.class))).thenReturn(1);

        service.consume(event("ORDER_CREATED", 3980L));

        verify(statsMapper).upsert(anyLong(), eq(10L),
                eq(LocalDate.of(2026, 10, 17)),
                eq(1L), eq(0L), eq(0L), eq(0L), eq(0L), eq(0L));
    }

    @Test
    @DisplayName("支付事件累加支付数与成交额")
    void consume_paid() {
        when(eventLogMapper.insertIgnore(anyLong(), eq("evt-1"), eq("ORDER_PAID"),
                eq(500L), eq(10L), any(LocalDate.class))).thenReturn(1);

        service.consume(event("ORDER_PAID", 3980L));

        verify(statsMapper).upsert(anyLong(), eq(10L),
                eq(LocalDate.of(2026, 10, 17)),
                eq(0L), eq(1L), eq(0L), eq(0L), eq(0L), eq(3980L));
    }

    @Test
    @DisplayName("重复事件不再累加")
    void consume_duplicate_skips() {
        when(eventLogMapper.insertIgnore(anyLong(), any(), any(), any(), any(), any()))
                .thenReturn(0);

        service.consume(event("ORDER_PAID", 3980L));

        verify(statsMapper, never()).upsert(anyLong(), any(), any(),
                anyLong(), anyLong(), anyLong(), anyLong(), anyLong(), anyLong());
    }

    @Test
    @DisplayName("未知事件类型忽略")
    void consume_unknownType() {
        service.consume(event("ORDER_UNKNOWN", 0L));

        verify(eventLogMapper, never()).insertIgnore(anyLong(), any(), any(), any(), any(), any());
        verify(statsMapper, never()).upsert(anyLong(), any(), any(),
                anyLong(), anyLong(), anyLong(), anyLong(), anyLong(), anyLong());
    }
}
