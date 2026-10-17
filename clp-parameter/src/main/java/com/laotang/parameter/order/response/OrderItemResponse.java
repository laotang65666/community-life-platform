package com.laotang.parameter.order.response;

/**
 * 订单明细项(下单时快照)。
 */
public record OrderItemResponse(
        String productId,
        String productName,
        String productImage,
        Long price,
        Integer quantity,
        Long amount
) {
}
