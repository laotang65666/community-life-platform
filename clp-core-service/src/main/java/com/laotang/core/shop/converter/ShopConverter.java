package com.laotang.core.shop.converter;

import com.laotang.common.util.DateTimeUtils;
import com.laotang.core.shop.entity.Shop;
import com.laotang.core.shop.entity.ShopType;
import com.laotang.parameter.shop.response.ShopBriefResponse;
import com.laotang.parameter.shop.response.ShopResponse;
import com.laotang.parameter.shop.response.ShopTypeResponse;

/**
 * Shop / ShopType 实体到 DTO 的转换。
 */
public final class ShopConverter {

    private ShopConverter() {
    }

    public static ShopResponse toResponse(Shop shop) {
        return new ShopResponse(
                String.valueOf(shop.getId()),
                String.valueOf(shop.getMerchantId()),
                String.valueOf(shop.getTypeId()),
                shop.getName(),
                shop.getAddress(),
                shop.getLat(),
                shop.getLng(),
                shop.getPhone(),
                shop.getOpenTime(),
                shop.getCloseTime(),
                shop.getStatus(),
                DateTimeUtils.toIso(shop.getCreatedAt()));
    }

    public static ShopBriefResponse toBrief(Shop shop) {
        return new ShopBriefResponse(
                String.valueOf(shop.getId()),
                String.valueOf(shop.getTypeId()),
                shop.getName(),
                shop.getAddress(),
                shop.getPhone(),
                shop.getStatus());
    }

    public static ShopTypeResponse toTypeResponse(ShopType type) {
        return new ShopTypeResponse(
                String.valueOf(type.getId()),
                type.getName(),
                type.getIcon(),
                type.getSort() == null ? 0 : type.getSort());
    }
}
