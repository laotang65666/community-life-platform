package com.laotang.parameter.marketing.response;

/**
 * 优惠券详情。金额单位:分。
 */
public record VoucherResponse(
        String id,
        String shopId,
        String title,
        String subTitle,
        String rules,
        Long payValue,
        Long actualValue,
        String type,
        String status,
        String beginTime,
        String endTime,
        Integer stock,
        Integer perUserLimit
) {
}
