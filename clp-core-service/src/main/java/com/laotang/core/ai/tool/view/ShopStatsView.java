package com.laotang.core.ai.tool.view;

/**
 * 店铺当日经营概览。
 */
public record ShopStatsView(
        String date,
        long todayOrderCount,
        long paidOrderCount,
        String gmvYuan,
        long pendingAcceptCount
) {
}
