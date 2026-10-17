package com.laotang.parameter.shop.response;

/**
 * 店铺分类。
 */
public record ShopTypeResponse(
        String id,
        String name,
        String icon,
        int sort
) {
}
