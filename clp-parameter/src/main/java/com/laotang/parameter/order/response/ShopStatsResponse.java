package com.laotang.parameter.order.response;

/**
 * 店铺当日经营概览。金额单位:分。
 *
 * @param date              统计日期
 * @param todayOrderCount   今日创建的全部订单数
 * @param paidOrderCount    今日已支付及之后状态的订单数
 * @param gmvAmount         今日成交额(PAID / ACCEPTED / COMPLETED)
 * @param pendingAcceptCount 当前待接单数
 */
public record ShopStatsResponse(
        String date,
        long todayOrderCount,
        long paidOrderCount,
        Long gmvAmount,
        long pendingAcceptCount
) {
}
