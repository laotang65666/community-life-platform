package com.laotang.core.stats.service;

import com.laotang.core.merchant.mapper.MerchantMapper;
import com.laotang.core.order.mapper.OrderMapper;
import com.laotang.core.order.service.IOrderService;
import com.laotang.core.shop.mapper.ShopMapper;
import com.laotang.core.stats.entity.ShopDailyStats;
import com.laotang.core.stats.mapper.ShopDailyStatsMapper;
import com.laotang.core.user.mapper.UserMapper;
import com.laotang.parameter.order.response.ShopStatsResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StatsQueryServiceTest {

    @Mock
    private IOrderService orderService;
    @Mock
    private ShopDailyStatsMapper dailyStatsMapper;
    @Mock
    private OrderMapper orderMapper;
    @Mock
    private UserMapper userMapper;
    @Mock
    private MerchantMapper merchantMapper;
    @Mock
    private ShopMapper shopMapper;

    private StatsQueryService service;

    @BeforeEach
    void setUp() {
        service = new StatsQueryService(
                orderService, dailyStatsMapper, orderMapper,
                userMapper, merchantMapper, shopMapper);
    }

    private ShopDailyStats stats(LocalDate date, long orders, long paid, long gmv) {
        ShopDailyStats stats = new ShopDailyStats();
        stats.setStatDate(date);
        stats.setOrderCount(orders);
        stats.setPaidCount(paid);
        stats.setCompletedCount(1L);
        stats.setCancelCount(0L);
        stats.setCloseCount(0L);
        stats.setGmv(gmv);
        return stats;
    }

    @Test
    @DisplayName("商户报表:当天实时数据 + 历史趋势")
    void shopReport_combinesTodayAndTrend() {
        when(orderService.shopStats(10L, 1L))
                .thenReturn(new ShopStatsResponse("2026-10-17", 5, 3, 9900L, 1));
        when(dailyStatsMapper.selectShopTrend(eq(10L), any(), any()))
                .thenReturn(List.of(stats(LocalDate.of(2026, 10, 16), 4, 2, 7960L)));

        var report = service.shopReport(10L, 1L, 7);

        assertThat(report.today().todayOrderCount()).isEqualTo(5);
        assertThat(report.today().gmvAmount()).isEqualTo(9900L);
        assertThat(report.trend()).hasSize(1);
        assertThat(report.trend().getFirst().gmvYuan()).isEqualTo("79.60");
    }

    @Test
    @DisplayName("管理看板:实时当天数据 + 平台趋势")
    void dashboardSummary() {
        when(userMapper.selectCount(isNull())).thenReturn(100L);
        when(merchantMapper.selectCount(any())).thenReturn(5L, 2L);
        when(shopMapper.selectCount(isNull())).thenReturn(4L);
        when(orderMapper.countCreatedSinceAll(any())).thenReturn(12L);
        when(orderMapper.sumPaidAmountSinceAll(any())).thenReturn(256800L);
        when(dailyStatsMapper.sumPlatformTrend(any(), any()))
                .thenReturn(List.of(stats(LocalDate.of(2026, 10, 16), 30, 20, 500000L)));

        var summary = service.dashboardSummary();

        assertThat(summary.userCount()).isEqualTo(100L);
        assertThat(summary.merchantCount()).isEqualTo(5L);
        assertThat(summary.shopCount()).isEqualTo(4L);
        assertThat(summary.todayOrders()).isEqualTo(12L);
        assertThat(summary.todayGmv()).isEqualTo(256800L);
        assertThat(summary.pendingMerchantAudits()).isEqualTo(2L);
        assertThat(summary.last7Days()).hasSize(1);
        assertThat(summary.last7Days().getFirst().gmvYuan()).isEqualTo("5000.00");
    }
}
