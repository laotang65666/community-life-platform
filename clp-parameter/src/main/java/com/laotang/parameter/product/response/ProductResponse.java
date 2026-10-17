package com.laotang.parameter.product.response;

/**
 * 商品详情。价格单位:分。
 */
public record ProductResponse(
        String id,
        String shopId,
        String name,
        Long price,
        Integer stock,
        Integer sold,
        String description,
        String image,
        String status,
        String createdAt
) {
}
