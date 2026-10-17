package com.laotang.core.ai.tool;

import com.laotang.common.exception.BusinessException;
import com.laotang.common.result.PageResult;
import com.laotang.core.ai.tool.view.OrderDetailView;
import com.laotang.core.ai.tool.view.OrderItemView;
import com.laotang.core.ai.tool.view.OrderView;
import com.laotang.core.ai.tool.view.PointsView;
import com.laotang.core.ai.tool.view.ProductView;
import com.laotang.core.ai.tool.view.ShopView;
import com.laotang.core.growth.service.IGrowthService;
import com.laotang.core.order.service.IOrderService;
import com.laotang.core.product.service.IProductService;
import com.laotang.core.shop.entity.Shop;
import com.laotang.core.shop.service.IShopService;
import com.laotang.parameter.order.response.OrderBriefResponse;
import com.laotang.parameter.order.response.OrderResponse;
import com.laotang.parameter.product.response.ProductBriefResponse;
import com.laotang.parameter.shop.response.ShopBriefResponse;
import com.laotang.parameter.shop.response.ShopResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.util.List;
import java.util.function.Supplier;

/**
 * 用户端 AI 工具。
 *
 * <p>安全约束:userId 由构造函数注入,所有查询都以它为过滤条件;
 * 工具参数里没有用户 ID,模型无法通过改参数越权查询他人数据。</p>
 */
@Slf4j
public class UserAiTools {

    private final Long userId;
    private final IOrderService orderService;
    private final IProductService productService;
    private final IShopService shopService;
    private final IGrowthService growthService;
    private final AiToolRecorder recorder;

    public UserAiTools(
            Long userId,
            IOrderService orderService,
            IProductService productService,
            IShopService shopService,
            IGrowthService growthService,
            AiToolRecorder recorder) {
        this.userId = userId;
        this.orderService = orderService;
        this.productService = productService;
        this.shopService = shopService;
        this.growthService = growthService;
        this.recorder = recorder;
    }

    @Tool(description = """
            查询当前登录用户最近的订单,返回订单号、店铺、金额、状态与下单时间。
            回答"我的订单""订单状态"等问题前必须先调用。
            status 可选: CREATED(待支付)、PAID(待接单)、ACCEPTED(已接单)、
            COMPLETED(已完成)、CANCELLED(已取消)、CLOSED(超时关闭);不传表示全部。
            """)
    public ToolResponse<List<OrderView>> queryMyOrders(
            @ToolParam(description = "订单状态,可选", required = false) String status,
            @ToolParam(description = "返回条数 1-10,默认 5", required = false) Integer limit) {

        int size = ToolFormats.limit(limit);
        String normalizedStatus = ToolFormats.trimToNull(status);
        return invoke("queryMyOrders", "status=" + normalizedStatus + ", limit=" + size, () -> {
            PageResult<OrderBriefResponse> page =
                    orderService.listMyOrders(userId, normalizedStatus, 1, size);
            List<OrderView> views = page.list().stream()
                    .map(order -> new OrderView(
                            order.orderNo(),
                            shopName(order.shopId()),
                            ToolFormats.yuan(order.totalAmount()),
                            order.status(),
                            ToolFormats.orderStatusText(order.status()),
                            order.createdAt()))
                    .toList();
            return ToolResponse.ok(views);
        });
    }

    @Tool(description = "按订单号查询当前用户自己的订单详情,包含商品明细与各节点时间。")
    public ToolResponse<OrderDetailView> queryMyOrderDetail(
            @ToolParam(description = "订单号") String orderNo) {

        return invoke("queryMyOrderDetail", "orderNo=" + orderNo, () -> {
            OrderResponse order = orderService.getMyOrderByNo(userId, orderNo);
            List<OrderItemView> items = order.items() == null ? List.of()
                    : order.items().stream()
                    .map(item -> new OrderItemView(
                            item.productName(),
                            ToolFormats.yuan(item.price()),
                            item.quantity(),
                            ToolFormats.yuan(item.amount())))
                    .toList();
            return ToolResponse.ok(new OrderDetailView(
                    order.orderNo(),
                    shopName(order.shopId()),
                    ToolFormats.yuan(order.totalAmount()),
                    order.status(),
                    ToolFormats.orderStatusText(order.status()),
                    order.createdAt(),
                    order.payTime(),
                    order.completeTime(),
                    items));
        });
    }

    @Tool(description = """
            按关键词搜索营业中的店铺,返回店铺 ID、名称、地址、电话与营业状态。
            需要营业时间等更多信息时,再用 queryShopInfo 查询详情。
            """)
    public ToolResponse<List<ShopView>> searchShops(
            @ToolParam(description = "店铺关键词,可为空", required = false) String keyword,
            @ToolParam(description = "返回条数 1-10,默认 5", required = false) Integer limit) {

        int size = ToolFormats.limit(limit);
        String normalizedKeyword = ToolFormats.trimToNull(keyword);
        return invoke("searchShops", "keyword=" + normalizedKeyword + ", limit=" + size, () -> {
            PageResult<ShopBriefResponse> page =
                    shopService.list(1, size, null, normalizedKeyword);
            List<ShopView> views = page.list().stream()
                    .map(shop -> new ShopView(
                            shop.id(),
                            shop.name(),
                            shop.address(),
                            shop.phone(),
                            null,
                            null,
                            shop.status(),
                            ToolFormats.shopStatusText(shop.status())))
                    .toList();
            return ToolResponse.ok(views);
        });
    }

    @Tool(description = "查询某家店铺的在售商品,返回商品 ID、名称、价格与库存。需先取得 shopId。")
    public ToolResponse<List<ProductView>> listShopProducts(
            @ToolParam(description = "店铺 ID") String shopId,
            @ToolParam(description = "商品关键词,可为空", required = false) String keyword,
            @ToolParam(description = "返回条数 1-10,默认 5", required = false) Integer limit) {

        int size = ToolFormats.limit(limit);
        String normalizedKeyword = ToolFormats.trimToNull(keyword);
        return invoke("listShopProducts",
                "shopId=" + shopId + ", keyword=" + normalizedKeyword + ", limit=" + size,
                () -> {
                    long id;
                    try {
                        id = Long.parseLong(shopId);
                    } catch (NumberFormatException ex) {
                        return ToolResponse.fail("店铺 ID 格式不正确");
                    }
                    PageResult<ProductBriefResponse> page =
                            productService.listByShop(id, 1, size, normalizedKeyword);
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

    @Tool(description = "查询店铺详情:地址、电话、营业时间与营业状态。")
    public ToolResponse<ShopView> queryShopInfo(
            @ToolParam(description = "店铺 ID") String shopId) {

        return invoke("queryShopInfo", "shopId=" + shopId, () -> {
            long id;
            try {
                id = Long.parseLong(shopId);
            } catch (NumberFormatException ex) {
                return ToolResponse.fail("店铺 ID 格式不正确");
            }
            ShopResponse shop = shopService.getDetail(id);
            return ToolResponse.ok(new ShopView(
                    shop.id(),
                    shop.name(),
                    shop.address(),
                    shop.phone(),
                    shop.openTime(),
                    shop.closeTime(),
                    shop.status(),
                    ToolFormats.shopStatusText(shop.status())));
        });
    }

    @Tool(description = "查询当前登录用户的积分余额。")
    public ToolResponse<PointsView> queryMyPoints() {
        return invoke("queryMyPoints", "", () ->
                ToolResponse.ok(new PointsView(growthService.points(userId).balance())));
    }

    private String shopName(String shopId) {
        try {
            Shop shop = shopService.getById(Long.parseLong(shopId));
            return ToolFormats.shopName(shop != null, shop == null ? null : shop.getName());
        } catch (NumberFormatException ex) {
            return "未知店铺";
        }
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
            log.error("用户端 AI 工具执行失败: {}", tool, ex);
            recorder.record(tool, arguments, false, "internal error", elapsed(start));
            return ToolResponse.fail("查询暂时失败,请稍后再试");
        }
    }

    private long elapsed(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000;
    }
}
