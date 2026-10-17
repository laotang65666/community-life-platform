package com.laotang.core.notification.message;

/**
 * Redis Pub/Sub 广播信封。
 *
 * @param userId  目标用户
 * @param payload 已经序列化好的 WebSocket 消息
 */
public record PushEnvelope(Long userId, String payload) {
}
