package com.laotang.parameter.stats.response;

import com.laotang.parameter.order.response.ShopStatsResponse;

import java.util.List;

/**
 * 商户经营报表。
 *
 * @param today 当天实时数据(直接查订单表)
 * @param trend 历史趋势(来自订单事件日统计)
 */
public record ShopStatsReportResponse(
        ShopStatsResponse today,
        List<ShopDailyTrendResponse> trend
) {
}
