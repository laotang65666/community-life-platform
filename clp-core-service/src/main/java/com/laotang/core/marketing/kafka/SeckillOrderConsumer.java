package com.laotang.core.marketing.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.laotang.core.marketing.service.SeckillOrderConsumeService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

/**
 * 秒杀订单消费者。
 *
 * <p>手动 ACK:业务成功才提交 offset。处理失败抛出异常,
 * 由 Kafka 的错误处理器重试。</p>
 */
@Component
@RequiredArgsConstructor
public class SeckillOrderConsumer {

    private static final Logger log = LoggerFactory.getLogger(SeckillOrderConsumer.class);

    private final ObjectMapper objectMapper;
    private final SeckillOrderConsumeService consumeService;

    @KafkaListener(
            topics = "clp-seckill-order",
            groupId = "clp-seckill-order",
            concurrency = "3")
    public void onMessage(String payload, Acknowledgment ack) {
        SeckillOrderMessage message;
        try {
            message = objectMapper.readValue(payload, SeckillOrderMessage.class);
        } catch (JsonProcessingException e) {
            // 无法解析的消息没有重试价值,记录后丢弃
            log.error("非法秒杀消息,丢弃 payload={}", payload, e);
            ack.acknowledge();
            return;
        }

        try {
            consumeService.consume(message);
            ack.acknowledge();
        } catch (Exception e) {
            log.error("消费秒杀消息失败 orderNo={}", message.orderNo(), e);
            throw e;
        }
    }
}
