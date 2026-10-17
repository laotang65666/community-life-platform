package com.laotang.core.notification.service;

import com.laotang.common.result.PageResult;
import com.laotang.parameter.notification.response.NotificationResponse;

/**
 * 通知服务。
 */
public interface INotificationService {

    /**
     * 保存通知并尝试实时推送。
     */
    void send(Long userId, String type, String title, String content, String bizId);

    PageResult<NotificationResponse> list(Long userId, long page, long size);

    long unreadCount(Long userId);

    void markRead(Long userId, Long notificationId);

    void markAllRead(Long userId);
}
