package com.laotang.core.ai.tool;

import com.laotang.common.exception.BusinessException;
import com.laotang.common.result.PageResult;
import com.laotang.core.ai.tool.view.OrderView;
import com.laotang.core.ai.tool.view.ProductView;
import com.laotang.core.ai.tool.view.ShopStatsView;
import com.laotang.core.ai.tool.view.ShopView;
import com.laotang.core.order.service.IOrderService;
import com.laotang.core.product.service.IProductService;
import com.laotang.core.shop.entity.Shop;
import com.laotang.core.shop.service.IShopService;
import com.laotang.parameter.order.response.OrderBriefResponse;
import com.laotang.parameter.order.response.ShopStatsResponse;
import com.laotang.parameter.product.response.ProductBriefResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.util.List;
import java.util.function.Supplier;

/**
 * 商户端 AI 工具。
 *
 * <p>merchantId / shopId 由工厂根据登录用户反查后注入,
 * 模型只能决定"查什么",不能决定"查哪家店"。</p>
 */
@Slf4j
public class MerchantAiTools {

    private final Long merchantId;
    private final Long shopId;
    private final IOrderService orderService;
    private final IProductService productService;
    private final IShopService shopService;
    private final AiToolRecorder recorder;

    public MerchantAiTools(
            Long merchantId,
            Long shopId,
            IOrderService orderService,
            IProductService productService,
            IShopService shopService,
            AiToolRecorder recorder) {
        this.merchantId = merchantId;
        this.shopId = shopId;
        this.orderService = orderService;
        this.productService = productService;
        this.shopService = shopService;
        this.recorder = recorder;
    }

    @Tool(description = "查询当前商户自己的店铺信息:名称、地址、电话、营业时间与营业状态。")
    public ToolResponse<ShopView> queryMyShop() {
        return invoke("queryMyShop", "", () -> {
            Shop shop = shopService.getById(shopId);
            if (shop == null) {
                return ToolResponse.fail("店铺不存在");
            }
            return ToolResponse.ok(new ShopView(
                    String.valueOf(shop.getId()),
                    shop.getName(),
                    shop.getAddress(),
                    shop.getPhone(),
                    shop.getOpenTime(),
                    shop.getCloseTime(),
                    shop.getStatus(),
                    ToolFormats.shopStatusText(shop.getStatus())));
        });
    }

    @Tool(description = """
            查询本店订单,返回订单号、金额、状态与下单时间。
            回答"今天有哪些订单""待接单"等问题前必须先调用。
            status 可选: CREATED、PAID(待接单)、ACCEPTED、COMPLETED、CANCELLED、CLOSED;
            不传表示全部。
            """)
    public ToolResponse<List<OrderView>> queryShopOrders(
            @ToolParam(description = "订单状态,可选", required = false) String status,
            @ToolParam(description = "返回条数 1-10,默认 5", required = false) Integer limit) {

        int size = ToolFormats.limit(limit);
        String normalizedStatus = ToolFormats.trimToNull(status);
        return invoke("queryShopOrders",
                "status=" + normalizedStatus + ", limit=" + size,
                () -> {
                    PageResult<OrderBriefResponse> page =
                            orderService.listShopOrders(
                                    shopId, merchantId, normalizedStatus, 1, size);
                    List<OrderView> views = page.list().stream()
                            .map(order -> new OrderView(
                                    order.orderNo(),
                                    null,
                                    ToolFormats.yuan(order.totalAmount()),
                                    order.status(),
                                    ToolFormats.orderStatusText(order.status()),
                                    order.createdAt()))
                            .toList();
                    return ToolResponse.ok(views);
                });
    }

    @Tool(description = "查询本店商品(含已下架),返回商品 ID、名称、价格、库存与状态。")
    public ToolResponse<List<ProductView>> queryShopProducts(
            @ToolParam(description = "返回条数 1-10,默认 5", required = false) Integer limit) {

        int size = ToolFormats.limit(limit);
        return invoke("queryShopProducts", "limit=" + size, () -> {
            PageResult<ProductBriefResponse> page =
                    productService.listManage(shopId, merchantId, 1, size);
            List<ProductView> views = page.list().stream()
                    .map(product -> new ProductView(
                            product.id(),
                            product.name(),
                            ToolFormats.yuan(product.price()),
                            product.stock(),
                            product.status(),
                            ToolFormats.productStatusText(product.status())))
                    .toList();
            return ToolResponse.ok(views);
        });
    }

    @Tool(description = "查询本店今日经营概览:今日订单数、成交订单数、成交额、待接单数。")
    public ToolResponse<ShopStatsView> queryShopStats() {
        return invoke("queryShopStats", "", () -> {
            ShopStatsResponse stats = orderService.shopStats(shopId, merchantId);
            return ToolResponse.ok(new ShopStatsView(
                    stats.date(),
                    stats.todayOrderCount(),
                    stats.paidOrderCount(),
                    ToolFormats.yuan(stats.gmvAmount()),
                    stats.pendingAcceptCount()));
        });
    }

    private <T> ToolResponse<T> invoke(
            String tool, String arguments, Supplier<ToolResponse<T>> action) {

        long start = System.nanoTime();
        try {
            ToolResponse<T> response = action.get();
            recorder.record(tool, arguments, response.success(), response.message(),
                    elapsed(start));
            return response;
        } catch (BusinessException ex) {
            recorder.record(tool, arguments, false, ex.getMessage(), elapsed(start));
            return ToolResponse.fail(ex.getMessage());
        } catch (Exception ex) {
            log.error("商户端 AI 工具执行失败: {}", tool, ex);
            recorder.record(tool, arguments, false, "internal error", elapsed(start));
            return ToolResponse.fail("查询暂时失败,请稍后再试");
        }
    }

    private long elapsed(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000;
    }
}
