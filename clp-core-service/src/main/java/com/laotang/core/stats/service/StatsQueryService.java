package com.laotang.core.stats.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.laotang.core.merchant.entity.Merchant;
import com.laotang.core.merchant.mapper.MerchantMapper;
import com.laotang.core.order.mapper.OrderMapper;
import com.laotang.core.order.service.IOrderService;
import com.laotang.core.shop.mapper.ShopMapper;
import com.laotang.core.stats.entity.ShopDailyStats;
import com.laotang.core.stats.mapper.ShopDailyStatsMapper;
import com.laotang.core.user.mapper.UserMapper;
import com.laotang.parameter.order.response.ShopStatsResponse;
import com.laotang.parameter.stats.response.DashboardSummaryResponse;
import com.laotang.parameter.stats.response.ShopDailyTrendResponse;
import com.laotang.parameter.stats.response.ShopStatsReportResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 经营数据查询。
 *
 * <p>当天数字走实时表,历史趋势走订单事件聚合表,避免在订单大表上做全量聚合。</p>
 */
@Service
@RequiredArgsConstructor
public class StatsQueryService {

    private static final int MAX_TREND_DAYS = 30;
    private static final int DEFAULT_TREND_DAYS = 7;

    private final IOrderService orderService;
    private final ShopDailyStatsMapper dailyStatsMapper;
    private final OrderMapper orderMapper;
    private final UserMapper userMapper;
    private final MerchantMapper merchantMapper;
    private final ShopMapper shopMapper;

    public ShopStatsReportResponse shopReport(Long shopId, Long merchantId, int days) {
        ShopStatsResponse today = orderService.shopStats(shopId, merchantId);

        int trendDays = normalizeDays(days);
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays(trendDays - 1L);
        List<ShopDailyTrendResponse> trend = dailyStatsMapper
                .selectShopTrend(shopId, start, end)
                .stream()
                .map(this::toTrend)
                .toList();

        return new ShopStatsReportResponse(today, trend);
    }

    public DashboardSummaryResponse dashboardSummary() {
        LocalDate today = LocalDate.now();
        LocalDateTime todayStart = today.atStartOfDay();

        long userCount = userMapper.selectCount(null);
        long merchantCount = merchantMapper.selectCount(
                new LambdaQueryWrapper<Merchant>()
                        .eq(Merchant::getAuditStatus, "APPROVED"));
        long shopCount = shopMapper.selectCount(null);
        long pendingAudits = merchantMapper.selectCount(
                new LambdaQueryWrapper<Merchant>()
                        .eq(Merchant::getAuditStatus, "PENDING"));

        long todayOrders = orderMapper.countCreatedSinceAll(todayStart);
        long todayGmv = orderMapper.sumPaidAmountSinceAll(todayStart);

        LocalDate start = today.minusDays(DEFAULT_TREND_DAYS - 1L);
        List<ShopDailyTrendResponse> trend = dailyStatsMapper
                .sumPlatformTrend(start, today)
                .stream()
                .map(this::toTrend)
                .toList();

        return new DashboardSummaryResponse(
                userCount, merchantCount, shopCount,
                todayOrders, todayGmv, pendingAudits, trend);
    }

    private int normalizeDays(int days) {
        if (days <= 0) {
            return DEFAULT_TREND_DAYS;
        }
        return Math.min(days, MAX_TREND_DAYS);
    }

    private ShopDailyTrendResponse toTrend(ShopDailyStats stats) {
        return new ShopDailyTrendResponse(
                stats.getStatDate() == null ? null : stats.getStatDate().toString(),
                nullToZero(stats.getOrderCount()),
                nullToZero(stats.getPaidCount()),
                nullToZero(stats.getCompletedCount()),
                nullToZero(stats.getCancelCount()),
                nullToZero(stats.getCloseCount()),
                toYuan(stats.getGmv()));
    }

    private long nullToZero(Long value) {
        return value == null ? 0L : value;
    }

    private String toYuan(Long cents) {
        return BigDecimal.valueOf(cents == null ? 0L : cents, 2).toPlainString();
    }
}
