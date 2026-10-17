package com.laotang.core.notification.ws;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 通知 WebSocket 处理器。
 *
 * <p>每个节点只维护本机连接;跨节点由 Redis Pub/Sub 广播后各自投递。</p>
 */
@Component
@RequiredArgsConstructor
public class NotificationWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(NotificationWebSocketHandler.class);

    private final ObjectMapper objectMapper;
    private final Map<Long, Set<WebSocketSession>> sessions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        Long userId = userId(session);
        if (userId == null) {
            closeQuietly(session);
            return;
        }
        sessions.computeIfAbsent(userId, key -> ConcurrentHashMap.newKeySet()).add(session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Long userId = userId(session);
        if (userId == null) {
            return;
        }
        Set<WebSocketSession> userSessions = sessions.get(userId);
        if (userSessions != null) {
            userSessions.remove(session);
            if (userSessions.isEmpty()) {
                sessions.remove(userId);
            }
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        try {
            JsonNode node = objectMapper.readTree(message.getPayload());
            if ("ping".equals(node.path("type").asText())) {
                sendText(session, "{\"type\":\"pong\"}");
            }
        } catch (Exception e) {
            log.warn("非法 WebSocket 消息 sessionId={}", session.getId());
        }
    }

    /**
     * 推送给当前节点上的用户连接。用户不在本节点时直接忽略。
     */
    public void sendToLocal(Long userId, String payload) {
        Set<WebSocketSession> userSessions = sessions.get(userId);
        if (userSessions == null || userSessions.isEmpty()) {
            return;
        }
        for (WebSocketSession session : userSessions) {
            if (!session.isOpen()) {
                userSessions.remove(session);
                continue;
            }
            sendText(session, payload);
        }
    }

    private void sendText(WebSocketSession session, String text) {
        try {
            // WebSocketSession 不支持并发发送,同一 session 加锁
            synchronized (session) {
                session.sendMessage(new TextMessage(text));
            }
        } catch (Exception e) {
            log.warn("WebSocket 发送失败 sessionId={}", session.getId(), e);
        }
    }

    private Long userId(WebSocketSession session) {
        Object userId = session.getAttributes().get("userId");
        return userId instanceof Long value ? value : null;
    }

    private void closeQuietly(WebSocketSession session) {
        try {
            session.close();
        } catch (Exception ignored) {
            // ignore
        }
    }
}
