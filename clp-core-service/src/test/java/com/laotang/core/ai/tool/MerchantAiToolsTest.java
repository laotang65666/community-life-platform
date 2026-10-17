package com.laotang.core.ai.tool;

import com.laotang.common.result.PageResult;
import com.laotang.core.order.service.IOrderService;
import com.laotang.core.product.service.IProductService;
import com.laotang.core.shop.service.IShopService;
import com.laotang.parameter.order.response.OrderBriefResponse;
import com.laotang.parameter.order.response.ShopStatsResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MerchantAiToolsTest {

    @Mock
    private IOrderService orderService;
    @Mock
    private IProductService productService;
    @Mock
    private IShopService shopService;

    private AiToolRecorder recorder;
    private MerchantAiTools tools;

    @BeforeEach
    void setUp() {
        recorder = new AiToolRecorder();
        tools = new MerchantAiTools(
                10L, 20L, orderService, productService, shopService, recorder);
    }

    @Test
    @DisplayName("经营概览:成交额转元")
    void queryShopStats_mapsAmount() {
        when(orderService.shopStats(20L, 10L))
                .thenReturn(new ShopStatsResponse("2026-10-17", 12, 8, 256800L, 3));

        var response = tools.queryShopStats();

        assertThat(response.success()).isTrue();
        assertThat(response.data().todayOrderCount()).isEqualTo(12);
        assertThat(response.data().paidOrderCount()).isEqualTo(8);
        assertThat(response.data().gmvYuan()).isEqualTo("2568.00");
        assertThat(response.data().pendingAcceptCount()).isEqualTo(3);
    }

    @Test
    @DisplayName("查本店订单使用注入的 shopId 与 merchantId")
    void queryShopOrders_usesInjectedIdentity() {
        OrderBriefResponse brief = new OrderBriefResponse(
                "100", "NO100", "20", 990L, "PAID", "2026-10-17T10:00:00+08:00");
        when(orderService.listShopOrders(20L, 10L, "PAID", 1, 5))
                .thenReturn(PageResult.of(List.of(brief), 1, 5, 1));

        var response = tools.queryShopOrders("PAID", null);

        assertThat(response.success()).isTrue();
        assertThat(response.data()).hasSize(1);
        assertThat(response.data().getFirst().amountYuan()).isEqualTo("9.90");
        assertThat(response.data().getFirst().statusText()).isEqualTo("已支付,待商家接单");
    }
}
