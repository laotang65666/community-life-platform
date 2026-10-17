package com.laotang.core.stats.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.laotang.core.order.entity.Order;
import com.laotang.core.outbox.service.OutboxService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 订单事件发布。
 *
 * <p>先写 Outbox,再由定时器投递 Kafka,保证"订单已落库、事件不丢"。
 * 事件属于统计侧链路,序列化失败只记录日志,不影响主交易。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventPublisher {

    public static final String TOPIC = "clp-order-event";
    public static final String BIZ_TYPE = "ORDER";

    private final OutboxService outboxService;
    private final ObjectMapper objectMapper;

    public void publish(OrderEventType type, Order order) {
        String eventId = UUID.randomUUID().toString();
        OrderEvent event = new OrderEvent(
                eventId,
                type.name(),
                order.getId(),
                order.getOrderNo(),
                order.getUserId(),
                order.getShopId(),
                order.getTotalAmount(),
                order.getCreatedAt(),
                LocalDateTime.now());

        try {
            String payload = objectMapper.writeValueAsString(event);
            outboxService.enqueue(
                    TOPIC, BIZ_TYPE, String.valueOf(order.getId()), payload, eventId);
        } catch (JsonProcessingException ex) {
            log.error("订单事件序列化失败, orderId={}, type={}", order.getId(), type, ex);
        }
    }
}
