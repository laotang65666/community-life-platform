package com.laotang.core.notification.converter;

import com.laotang.common.util.DateTimeUtils;
import com.laotang.core.notification.entity.Notification;
import com.laotang.parameter.notification.response.NotificationResponse;

/**
 * Notification 实体到 DTO 的转换。
 */
public final class NotificationConverter {

    private NotificationConverter() {
    }

    public static NotificationResponse toResponse(Notification notification) {
        return new NotificationResponse(
                String.valueOf(notification.getId()),
                notification.getType(),
                notification.getTitle(),
                notification.getContent(),
                notification.getBizId(),
                notification.getIsRead() == null ? 0 : notification.getIsRead(),
                DateTimeUtils.toIso(notification.getCreatedAt()));
    }
}
