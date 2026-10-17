package com.laotang.core.ai.tool.view;

/**
 * 订单明细项(下单快照)。
 */
public record OrderItemView(
        String productName,
        String priceYuan,
        int quantity,
        String amountYuan
) {
}
