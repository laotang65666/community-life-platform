package com.laotang.core.product.converter;

import com.laotang.common.util.DateTimeUtils;
import com.laotang.core.product.entity.Product;
import com.laotang.parameter.product.response.ProductBriefResponse;
import com.laotang.parameter.product.response.ProductResponse;

/**
 * Product 实体到 DTO 的转换。
 */
public final class ProductConverter {

    private ProductConverter() {
    }

    public static ProductResponse toResponse(Product product) {
        return new ProductResponse(
                String.valueOf(product.getId()),
                String.valueOf(product.getShopId()),
                product.getName(),
                product.getPrice(),
                product.getStock(),
                product.getSold(),
                product.getDescription(),
                product.getImage(),
                product.getStatus(),
                DateTimeUtils.toIso(product.getCreatedAt()));
    }

    public static ProductBriefResponse toBrief(Product product) {
        return new ProductBriefResponse(
                String.valueOf(product.getId()),
                String.valueOf(product.getShopId()),
                product.getName(),
                product.getPrice(),
                product.getStock(),
                product.getImage(),
                product.getStatus());
    }
}
