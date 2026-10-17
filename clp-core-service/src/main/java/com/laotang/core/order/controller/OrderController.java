package com.laotang.core.order.controller;

import com.laotang.common.annotation.RequireRole;
import com.laotang.common.context.UserContext;
import com.laotang.common.enums.RoleEnum;
import com.laotang.common.exception.BusinessException;
import com.laotang.common.exception.ErrorCode;
import com.laotang.common.result.PageResult;
import com.laotang.common.result.Result;
import com.laotang.common.util.PageUtils;
import com.laotang.core.merchant.entity.Merchant;
import com.laotang.core.merchant.service.IMerchantService;
import com.laotang.core.order.service.IOrderService;
import com.laotang.parameter.order.request.CreateOrderRequest;
import com.laotang.parameter.order.response.OrderBriefResponse;
import com.laotang.parameter.order.response.OrderResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 订单接口。
 */
@Tag(name = "订单")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class OrderController {

    private final IOrderService orderService;
    private final IMerchantService merchantService;

    @Operation(summary = "创建订单")
    @PostMapping("/orders")
    @RequireRole(RoleEnum.USER)
    public Result<OrderResponse> create(
            @Parameter(description = "客户端生成的幂等键,同一次提交必须复用")
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody CreateOrderRequest request) {
        return Result.ok(orderService.create(UserContext.getUserId(), idempotencyKey, request));
    }

    @Operation(summary = "我的订单")
    @GetMapping("/orders")
    @RequireRole(RoleEnum.USER)
    public Result<PageResult<OrderBriefResponse>> myOrders(
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "page", defaultValue = "1") long page,
            @RequestParam(value = "size", defaultValue = "10") long size) {
        return Result.ok(orderService.listMyOrders(
                UserContext.getUserId(), status,
                PageUtils.normalizePage(page), PageUtils.normalizeSize(size)));
    }

    @Operation(summary = "订单详情")
    @GetMapping("/orders/{orderId}")
    @RequireRole(RoleEnum.USER)
    public Result<OrderResponse> detail(@PathVariable("orderId") Long orderId) {
        return Result.ok(orderService.getDetail(
                orderId, UserContext.getUserId(), currentMerchantId()));
    }

    @Operation(summary = "取消订单")
    @PostMapping("/orders/{orderId}/cancel")
    @RequireRole(RoleEnum.USER)
    public Result<Void> cancel(@PathVariable("orderId") Long orderId) {
        orderService.cancel(orderId, UserContext.getUserId());
        return Result.ok();
    }

    @Operation(summary = "支付订单(模拟)")
    @PostMapping("/orders/{orderId}/pay")
    @RequireRole(RoleEnum.USER)
    public Result<Void> pay(@PathVariable("orderId") Long orderId) {
        orderService.pay(orderId, UserContext.getUserId());
        return Result.ok();
    }

    @Operation(summary = "商户接单")
    @PostMapping("/orders/{orderId}/accept")
    @RequireRole(RoleEnum.MERCHANT)
    public Result<Void> accept(@PathVariable("orderId") Long orderId) {
        Merchant merchant = requireMerchant();
        orderService.accept(orderId, merchant.getId());
        return Result.ok();
    }

    @Operation(summary = "商户完成订单")
    @PostMapping("/orders/{orderId}/complete")
    @RequireRole(RoleEnum.MERCHANT)
    public Result<Void> complete(@PathVariable("orderId") Long orderId) {
        Merchant merchant = requireMerchant();
        orderService.complete(orderId, merchant.getId());
        return Result.ok();
    }

    @Operation(summary = "店铺订单")
    @GetMapping("/shops/{shopId}/orders")
    @RequireRole(RoleEnum.MERCHANT)
    public Result<PageResult<OrderBriefResponse>> shopOrders(
            @PathVariable("shopId") Long shopId,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "page", defaultValue = "1") long page,
            @RequestParam(value = "size", defaultValue = "10") long size) {
        Merchant merchant = requireMerchant();
        return Result.ok(orderService.listShopOrders(
                shopId, merchant.getId(), status,
                PageUtils.normalizePage(page), PageUtils.normalizeSize(size)));
    }

    private Merchant requireMerchant() {
        Merchant merchant = merchantService.getApprovedByUserId(UserContext.getUserId());
        if (merchant == null) {
            throw new BusinessException(ErrorCode.MERCHANT_NOT_APPROVED);
        }
        return merchant;
    }

    private Long currentMerchantId() {
        Merchant merchant = merchantService.getApprovedByUserId(UserContext.getUserId());
        return merchant == null ? null : merchant.getId();
    }
}
