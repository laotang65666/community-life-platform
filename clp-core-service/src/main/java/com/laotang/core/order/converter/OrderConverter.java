package com.laotang.core.order.converter;

import com.laotang.common.util.DateTimeUtils;
import com.laotang.core.order.entity.Order;
import com.laotang.core.order.entity.OrderItem;
import com.laotang.parameter.order.response.OrderBriefResponse;
import com.laotang.parameter.order.response.OrderItemResponse;
import com.laotang.parameter.order.response.OrderResponse;

import java.util.List;

/**
 * Order / OrderItem 实体到 DTO 的转换。
 */
public final class OrderConverter {

    private OrderConverter() {
    }

    public static OrderResponse toResponse(Order order, List<OrderItem> items) {
        List<OrderItemResponse> itemResponses = items.stream()
                .map(OrderConverter::toItemResponse)
                .toList();

        return new OrderResponse(
                String.valueOf(order.getId()),
                order.getOrderNo(),
                String.valueOf(order.getShopId()),
                order.getTotalAmount(),
                order.getStatus(),
                order.getRemark(),
                itemResponses,
                DateTimeUtils.toIso(order.getPayTime()),
                DateTimeUtils.toIso(order.getAcceptTime()),
                DateTimeUtils.toIso(order.getCompleteTime()),
                DateTimeUtils.toIso(order.getCancelTime()),
                order.getCancelReason(),
                DateTimeUtils.toIso(order.getCreatedAt()));
    }

    public static OrderItemResponse toItemResponse(OrderItem item) {
        return new OrderItemResponse(
                String.valueOf(item.getProductId()),
                item.getProductName(),
                item.getProductImage(),
                item.getPrice(),
                item.getQuantity(),
                item.getAmount());
    }

    public static OrderBriefResponse toBrief(Order order) {
        return new OrderBriefResponse(
                String.valueOf(order.getId()),
                order.getOrderNo(),
                String.valueOf(order.getShopId()),
                order.getTotalAmount(),
                order.getStatus(),
                DateTimeUtils.toIso(order.getCreatedAt()));
    }
}
