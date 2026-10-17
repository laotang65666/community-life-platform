package com.laotang.core.ai.tool.view;

/**
 * 店铺信息。
 */
public record ShopView(
        String shopId,
        String name,
        String address,
        String phone,
        String openTime,
        String closeTime,
        String status,
        String statusText
) {
}
