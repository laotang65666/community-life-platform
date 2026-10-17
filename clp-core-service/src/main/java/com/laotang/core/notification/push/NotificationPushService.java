package com.laotang.core.notification.push;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.laotang.common.constant.RedisKey;
import com.laotang.common.util.DateTimeUtils;
import com.laotang.core.notification.entity.Notification;
import com.laotang.core.notification.mapper.NotificationMapper;
import com.laotang.core.notification.message.PushEnvelope;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 通知保存 + 跨节点广播。
 *
 * <p>先落库(离线也能查),再通过 Redis Pub/Sub 广播。
 * 每个节点收到广播后,只推给自己持有的 WebSocket 连接。</p>
 */
@Service
@RequiredArgsConstructor
public class NotificationPushService {

    private static final Logger log = LoggerFactory.getLogger(NotificationPushService.class);

    private final NotificationMapper notificationMapper;
    private final RedissonClient redisson;
    private final ObjectMapper objectMapper;

    public void send(Long userId, String type, String title, String content, String bizId) {
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setType(type);
        notification.setTitle(title);
        notification.setContent(content);
        notification.setBizId(bizId == null ? "" : bizId);
        notification.setIsRead(0);
        notification.setCreatedAt(LocalDateTime.now());

        try {
            notificationMapper.insert(notification);
        } catch (Exception e) {
            log.error("保存通知失败 userId={} type={}", userId, type, e);
            return;
        }

        try {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("notificationId", String.valueOf(notification.getId()));
            data.put("title", title);
            data.put("content", content);
            data.put("bizId", bizId == null ? "" : bizId);
            data.put("createdAt", DateTimeUtils.toIso(notification.getCreatedAt()));

            String payload = objectMapper.writeValueAsString(
                    Map.of("type", type, "data", data));
            String envelope = objectMapper.writeValueAsString(
                    new PushEnvelope(userId, payload));

            redisson.getTopic(RedisKey.NOTIFY_CHANNEL, StringCodec.INSTANCE)
                    .publish(envelope);
        } catch (Exception e) {
            log.error("广播通知失败 userId={} type={}", userId, type, e);
        }
    }
}
