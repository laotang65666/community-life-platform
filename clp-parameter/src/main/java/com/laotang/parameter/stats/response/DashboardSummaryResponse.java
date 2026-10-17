package com.laotang.parameter.stats.response;

import java.util.List;

/**
 * 管理后台数据看板。
 *
 * <p>当天数据直接查业务表保证实时;趋势来自订单事件日统计。</p>
 */
public record DashboardSummaryResponse(
        long userCount,
        long merchantCount,
        long shopCount,
        long todayOrders,
        long todayGmv,
        long pendingMerchantAudits,
        List<ShopDailyTrendResponse> last7Days
) {
}
