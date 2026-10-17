package com.laotang.parameter.order.response;

/**
 * 订单列表项。
 */
public record OrderBriefResponse(
        String id,
        String orderNo,
        String shopId,
        Long totalAmount,
        String status,
        String createdAt
) {
}
