package com.laotang.core.ai.tool;

import com.laotang.common.exception.BusinessException;
import com.laotang.common.exception.ErrorCode;
import com.laotang.common.result.PageResult;
import com.laotang.core.growth.service.IGrowthService;
import com.laotang.core.order.service.IOrderService;
import com.laotang.core.product.service.IProductService;
import com.laotang.core.shop.entity.Shop;
import com.laotang.core.shop.service.IShopService;
import com.laotang.parameter.growth.response.PointsResponse;
import com.laotang.parameter.order.response.OrderBriefResponse;
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
class UserAiToolsTest {

    @Mock
    private IOrderService orderService;
    @Mock
    private IProductService productService;
    @Mock
    private IShopService shopService;
    @Mock
    private IGrowthService growthService;

    private AiToolRecorder recorder;
    private UserAiTools tools;

    @BeforeEach
    void setUp() {
        recorder = new AiToolRecorder();
        tools = new UserAiTools(1L, orderService, productService, shopService, growthService, recorder);
    }

    @Test
    @DisplayName("查我的订单:金额转元、状态转中文、补店铺名,并记录调用")
    void queryMyOrders_mapsFields() {
        OrderBriefResponse brief = new OrderBriefResponse(
                "100", "NO100", "9", 1250L, "PAID", "2026-10-17T10:00:00+08:00");
        when(orderService.listMyOrders(1L, null, 1, 5))
                .thenReturn(PageResult.of(List.of(brief), 1, 5, 1));

        Shop shop = new Shop();
        shop.setId(9L);
        shop.setName("小区超市");
        when(shopService.getById(9L)).thenReturn(shop);

        ToolResponse<List<com.laotang.core.ai.tool.view.OrderView>> response =
                tools.queryMyOrders(null, null);

        assertThat(response.success()).isTrue();
        assertThat(response.data()).hasSize(1);
        assertThat(response.data().getFirst().amountYuan()).isEqualTo("12.50");
        assertThat(response.data().getFirst().statusText()).isEqualTo("已支付,待商家接单");
        assertThat(response.data().getFirst().shopName()).isEqualTo("小区超市");

        assertThat(recorder.calls()).hasSize(1);
        assertThat(recorder.calls().getFirst().tool()).isEqualTo("queryMyOrders");
        assertThat(recorder.calls().getFirst().success()).isTrue();
    }

    @Test
    @DisplayName("业务异常转成 success=false,不中断对话")
    void queryMyOrderDetail_businessError() {
        when(orderService.getMyOrderByNo(1L, "NOPE"))
                .thenThrow(new BusinessException(ErrorCode.ORDER_NOT_FOUND));

        var response = tools.queryMyOrderDetail("NOPE");

        assertThat(response.success()).isFalse();
        assertThat(response.message()).isEqualTo("订单不存在");
        assertThat(recorder.calls().getFirst().success()).isFalse();
    }

    @Test
    @DisplayName("非法店铺 ID 返回可读错误")
    void listShopProducts_invalidShopId() {
        var response = tools.listShopProducts("abc", null, null);

        assertThat(response.success()).isFalse();
        assertThat(response.message()).contains("店铺 ID");
    }

    @Test
    @DisplayName("查积分使用服务端注入的 userId")
    void queryMyPoints_usesInjectedUserId() {
        when(growthService.points(1L)).thenReturn(new PointsResponse(55));

        var response = tools.queryMyPoints();

        assertThat(response.success()).isTrue();
        assertThat(response.data().balance()).isEqualTo(55);
    }
}
