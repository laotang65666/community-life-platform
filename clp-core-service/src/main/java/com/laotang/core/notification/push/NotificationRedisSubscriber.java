package com.laotang.core.notification.push;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.laotang.common.constant.RedisKey;
import com.laotang.core.notification.message.PushEnvelope;
import com.laotang.core.notification.ws.NotificationWebSocketHandler;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 订阅通知广播频道。
 */
@Component
@RequiredArgsConstructor
public class NotificationRedisSubscriber {

    private static final Logger log = LoggerFactory.getLogger(NotificationRedisSubscriber.class);

    private final RedissonClient redisson;
    private final ObjectMapper objectMapper;
    private final NotificationWebSocketHandler handler;

    @PostConstruct
    public void subscribe() {
        RTopic topic = redisson.getTopic(RedisKey.NOTIFY_CHANNEL, StringCodec.INSTANCE);
        topic.addListener(String.class, (channel, message) -> {
            try {
                PushEnvelope envelope = objectMapper.readValue(message, PushEnvelope.class);
                handler.sendToLocal(envelope.userId(), envelope.payload());
            } catch (Exception e) {
                log.error("通知广播解析失败", e);
            }
        });
    }
}
