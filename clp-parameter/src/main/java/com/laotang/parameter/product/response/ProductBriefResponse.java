package com.laotang.parameter.product.response;

/**
 * 商品列表项。价格单位:分。
 */
public record ProductBriefResponse(
        String id,
        String shopId,
        String name,
        Long price,
        Integer stock,
        String image,
        String status
) {
}
