package com.laotang.core.stats.event;

import java.time.LocalDateTime;

/**
 * 订单领域事件。
 *
 * <p>{@code orderCreatedAt} 决定统计归属日期:订单创建当天。
 * 这样"今日订单/今日成交额"与实时 SQL 的口径一致。</p>
 */
public record OrderEvent(
        String eventId,
        String eventType,
        Long orderId,
        String orderNo,
        Long userId,
        Long shopId,
        Long amount,
        LocalDateTime orderCreatedAt,
        LocalDateTime occurredAt
) {
}
