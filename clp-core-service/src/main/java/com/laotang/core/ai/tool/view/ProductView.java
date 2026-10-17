package com.laotang.core.ai.tool.view;

/**
 * 商品信息。
 */
public record ProductView(
        String productId,
        String name,
        String priceYuan,
        Integer stock,
        String status,
        String statusText
) {
}
