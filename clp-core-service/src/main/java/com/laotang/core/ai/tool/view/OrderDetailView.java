package com.laotang.core.ai.tool.view;

import java.util.List;

/**
 * 订单详情。
 */
public record OrderDetailView(
        String orderNo,
        String shopName,
        String amountYuan,
        String status,
        String statusText,
        String createdAt,
        String payTime,
        String completeTime,
        List<OrderItemView> items
) {
}
