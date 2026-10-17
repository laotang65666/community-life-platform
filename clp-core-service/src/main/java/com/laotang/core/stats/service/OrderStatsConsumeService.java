package com.laotang.core.stats.service;

import com.laotang.core.stats.entity.OrderEventLog;
import com.laotang.core.stats.event.OrderEvent;
import com.laotang.core.stats.event.OrderEventType;
import com.laotang.core.stats.mapper.OrderEventLogMapper;
import com.laotang.core.stats.mapper.ShopDailyStatsMapper;
import com.laotang.id.SnowflakeIdGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * 订单事件消费:去重 + 日统计累加。
 *
 * <p>去重表和统计累加在同一个事务里:累加失败时去重记录一起回滚,
 * Kafka 重试后仍能重新处理。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderStatsConsumeService {

    private final OrderEventLogMapper eventLogMapper;
    private final ShopDailyStatsMapper statsMapper;
    private final SnowflakeIdGenerator snowflakeIdGenerator;

    @Transactional(rollbackFor = Exception.class)
    public void consume(OrderEvent event) {
        if (event.eventId() == null || event.orderId() == null
                || event.shopId() == null || event.orderCreatedAt() == null) {
            log.warn("订单事件字段不完整,忽略: {}", event);
            return;
        }

        OrderEventType type;
        try {
            type = OrderEventType.valueOf(event.eventType());
        } catch (IllegalArgumentException ex) {
            log.warn("未知订单事件类型,忽略: {}", event.eventType());
            return;
        }

        LocalDate statDate = event.orderCreatedAt().toLocalDate();

        int inserted = eventLogMapper.insertIgnore(
                snowflakeIdGenerator.nextId(),
                event.eventId(),
                type.name(),
                event.orderId(),
                event.shopId(),
                statDate);
        if (inserted == 0) {
            // 重复消费,直接返回
            return;
        }

        long amount = event.amount() == null ? 0L : event.amount();
        statsMapper.upsert(
                snowflakeIdGenerator.nextId(),
                event.shopId(),
                statDate,
                type == OrderEventType.ORDER_CREATED ? 1 : 0,
                type == OrderEventType.ORDER_PAID ? 1 : 0,
                type == OrderEventType.ORDER_COMPLETED ? 1 : 0,
                type == OrderEventType.ORDER_CANCELLED ? 1 : 0,
                type == OrderEventType.ORDER_CLOSED ? 1 : 0,
                type == OrderEventType.ORDER_PAID ? amount : 0);
    }
}
