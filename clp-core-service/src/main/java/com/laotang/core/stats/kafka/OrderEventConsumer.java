package com.laotang.core.stats.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.laotang.core.outbox.service.OutboxService;
import com.laotang.core.stats.event.OrderEvent;
import com.laotang.core.stats.event.OrderEventPublisher;
import com.laotang.core.stats.service.OrderStatsConsumeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

/**
 * 订单事件消费者。
 *
 * <p>手动 ACK:统计写入成功才提交 offset;失败抛出异常由 Kafka 重试。
 * 重复消息由 tb_order_event_log 的唯一索引兜底。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventConsumer {

    private final ObjectMapper objectMapper;
    private final OrderStatsConsumeService consumeService;
    private final OutboxService outboxService;

    @KafkaListener(
            topics = OrderEventPublisher.TOPIC,
            groupId = "clp-order-stats",
            concurrency = "3")
    public void onMessage(String payload, Acknowledgment ack) {
        OrderEvent event;
        try {
            event = objectMapper.readValue(payload, OrderEvent.class);
        } catch (JsonProcessingException ex) {
            // 无法解析的消息没有重试价值
            log.error("非法订单事件,丢弃 payload={}", payload, ex);
            ack.acknowledge();
            return;
        }

        try {
            consumeService.consume(event);
            outboxService.markAcked(event.eventId());
            ack.acknowledge();
        } catch (Exception ex) {
            log.error("消费订单事件失败 eventId={}, type={}",
                    event.eventId(), event.eventType(), ex);
            throw ex;
        }
    }
}
