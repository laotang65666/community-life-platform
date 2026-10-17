package com.laotang.parameter.shop.response;

/**
 * 店铺列表项。
 */
public record ShopBriefResponse(
        String id,
        String typeId,
        String name,
        String address,
        String phone,
        String status
) {
}
