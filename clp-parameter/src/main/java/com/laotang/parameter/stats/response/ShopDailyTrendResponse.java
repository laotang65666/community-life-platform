package com.laotang.parameter.stats.response;

/**
 * 单日经营数据。
 *
 * @param gmvYuan 成交额,单位元,两位小数
 */
public record ShopDailyTrendResponse(
        String date,
        long orderCount,
        long paidCount,
        long completedCount,
        long cancelCount,
        long closeCount,
        String gmvYuan
) {
}
