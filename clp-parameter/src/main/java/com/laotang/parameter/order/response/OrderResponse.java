package com.laotang.parameter.order.response;

import java.util.List;

/**
 * 订单详情。金额单位:分。
 */
public record OrderResponse(
        String id,
        String orderNo,
        String shopId,
        Long totalAmount,
        String status,
        String remark,
        List<OrderItemResponse> items,
        String payTime,
        String acceptTime,
        String completeTime,
        String cancelTime,
        String cancelReason,
        String createdAt
) {
}
