package com.laotang.parameter.notification.response;

/**
 * 通知。
 */
public record NotificationResponse(
        String id,
        String type,
        String title,
        String content,
        String bizId,
        int isRead,
        String createdAt
) {
}
