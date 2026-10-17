package com.laotang.parameter.marketing.response;

/**
 * 优惠券列表项。金额单位:分。
 */
public record VoucherBriefResponse(
        String id,
        String shopId,
        String title,
        String subTitle,
        Long payValue,
        Long actualValue,
        String status,
        String beginTime,
        String endTime
) {
}
