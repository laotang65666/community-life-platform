package com.laotang.parameter.marketing.response;

/**
 * 秒杀订单。
 */
public record VoucherOrderResponse(
        String id,
        String orderNo,
        String voucherId,
        Long amount,
        String status,
        String createdAt
) {
}
