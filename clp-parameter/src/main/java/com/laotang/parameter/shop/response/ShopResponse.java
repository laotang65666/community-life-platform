package com.laotang.parameter.shop.response;

import java.math.BigDecimal;

/**
 * 店铺详情。
 */
public record ShopResponse(
        String id,
        String merchantId,
        String typeId,
        String name,
        String address,
        BigDecimal lat,
        BigDecimal lng,
        String phone,
        String openTime,
        String closeTime,
        String status,
        String createdAt
) {
}
