package com.laotang.core.outbox.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.laotang.core.outbox.entity.OutboxMessage;
import com.laotang.core.outbox.mapper.OutboxMessageMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Outbox 本地消息表。
 *
 * <p>先把消息写库(PENDING),再由定时器投递到 Kafka。
 * 解决了"业务成功但消息没发出去"的问题;即使应用在投递前宕机,
 * 重启后定时器仍会把 PENDING 消息补发。</p>
 */
@Service
@RequiredArgsConstructor
public class OutboxService {

    private static final Logger log = LoggerFactory.getLogger(OutboxService.class);

    private static final String PENDING = "PENDING";
    private static final String SENT = "SENT";
    private static final String ACKED = "ACKED";
    private static final String FAILED = "FAILED";

    private static final int MAX_RETRY = 10;
    private static final int BATCH_SIZE = 100;
    private static final long SEND_TIMEOUT_SECONDS = 3;

    private final OutboxMessageMapper outboxMessageMapper;
    private final KafkaTemplate<String, String> kafkaTemplate;

    /**
     * 写一条待投递消息,返回 messageId。
     */
    public String enqueue(String topic, String bizType, String bizId, String payload) {
        return enqueue(topic, bizType, bizId, payload, UUID.randomUUID().toString());
    }

    /**
     * 使用指定 messageId 入队。消息体里需要带上同一个 messageId,消费端才能回写 ACKED。
     */
    public String enqueue(String topic, String bizType, String bizId, String payload, String messageId) {
        OutboxMessage message = new OutboxMessage();
        message.setMessageId(messageId);
        message.setTopic(topic);
        message.setBizType(bizType);
        message.setBizId(bizId);
        message.setPayload(payload);
        message.setStatus(PENDING);
        message.setRetryCount(0);
        message.setNextRetryAt(LocalDateTime.now());
        outboxMessageMapper.insert(message);

        return messageId;
    }

    /**
     * 每秒投递一次待发送消息。
     */
    @Scheduled(fixedDelay = 1000L)
    public void dispatch() {
        List<OutboxMessage> messages = outboxMessageMapper.selectList(
                new LambdaQueryWrapper<OutboxMessage>()
                        .eq(OutboxMessage::getStatus, PENDING)
                        .and(w -> w.isNull(OutboxMessage::getNextRetryAt)
                                .or()
                                .le(OutboxMessage::getNextRetryAt, LocalDateTime.now()))
                        .orderByAsc(OutboxMessage::getId)
                        .last("LIMIT " + BATCH_SIZE));

        for (OutboxMessage message : messages) {
            try {
                kafkaTemplate.send(message.getTopic(), message.getMessageId(), message.getPayload())
                        .get(SEND_TIMEOUT_SECONDS, TimeUnit.SECONDS);

                outboxMessageMapper.update(null, new LambdaUpdateWrapper<OutboxMessage>()
                        .eq(OutboxMessage::getId, message.getId())
                        .set(OutboxMessage::getStatus, SENT)
                        .set(OutboxMessage::getNextRetryAt, null));
            } catch (Exception e) {
                int retryCount = message.getRetryCount() == null ? 0 : message.getRetryCount();
                retryCount++;

                String status = retryCount >= MAX_RETRY ? FAILED : PENDING;
                long backoffSeconds = Math.min(300L, 1L << Math.min(retryCount, 8));

                outboxMessageMapper.update(null, new LambdaUpdateWrapper<OutboxMessage>()
                        .eq(OutboxMessage::getId, message.getId())
                        .set(OutboxMessage::getRetryCount, retryCount)
                        .set(OutboxMessage::getStatus, status)
                        .set(OutboxMessage::getNextRetryAt, LocalDateTime.now().plusSeconds(backoffSeconds)));

                log.error("Outbox 投递失败 messageId={} retry={}", message.getMessageId(), retryCount, e);
            }
        }
    }

    /**
     * 消费端确认。
     */
    public void markAcked(String messageId) {
        outboxMessageMapper.update(null, new LambdaUpdateWrapper<OutboxMessage>()
                .eq(OutboxMessage::getMessageId, messageId)
                .set(OutboxMessage::getStatus, ACKED));
    }
}
