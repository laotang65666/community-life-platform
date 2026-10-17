package com.laotang.core.marketing.controller;

import com.laotang.common.annotation.RequireRole;
import com.laotang.common.context.UserContext;
import com.laotang.common.enums.RoleEnum;
import com.laotang.common.exception.BusinessException;
import com.laotang.common.exception.ErrorCode;
import com.laotang.common.result.PageResult;
import com.laotang.common.result.Result;
import com.laotang.common.util.PageUtils;
import com.laotang.core.marketing.service.ISeckillService;
import com.laotang.core.marketing.service.IVoucherService;
import com.laotang.core.merchant.entity.Merchant;
import com.laotang.core.merchant.service.IMerchantService;
import com.laotang.parameter.marketing.request.CreateVoucherRequest;
import com.laotang.parameter.marketing.request.SeckillOrderRequest;
import com.laotang.parameter.marketing.response.SeckillOrderResponse;
import com.laotang.parameter.marketing.response.SeckillTokenResponse;
import com.laotang.parameter.marketing.response.VoucherBriefResponse;
import com.laotang.parameter.marketing.response.VoucherOrderResponse;
import com.laotang.parameter.marketing.response.VoucherResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 优惠券与秒杀接口。
 */
@Tag(name = "优惠券 / 秒杀")
@RestController
@RequestMapping("/api/v1/vouchers")
@RequiredArgsConstructor
public class VoucherController {

    private final IVoucherService voucherService;
    private final ISeckillService seckillService;
    private final IMerchantService merchantService;

    @Operation(summary = "商户创建秒杀券")
    @PostMapping
    @RequireRole(RoleEnum.MERCHANT)
    public Result<VoucherResponse> create(@Valid @RequestBody CreateVoucherRequest request) {
        Merchant merchant = requireMerchant();
        return Result.ok(voucherService.create(merchant.getId(), request));
    }

    @Operation(summary = "优惠券列表")
    @GetMapping
    public Result<PageResult<VoucherBriefResponse>> list(
            @RequestParam(value = "shopId", required = false) Long shopId,
            @RequestParam(value = "page", defaultValue = "1") long page,
            @RequestParam(value = "size", defaultValue = "10") long size) {
        return Result.ok(voucherService.list(
                shopId, PageUtils.normalizePage(page), PageUtils.normalizeSize(size)));
    }

    @Operation(summary = "优惠券详情")
    @GetMapping("/{voucherId}")
    public Result<VoucherResponse> detail(@PathVariable("voucherId") Long voucherId) {
        return Result.ok(voucherService.getDetail(voucherId));
    }

    @Operation(summary = "申请秒杀令牌")
    @PostMapping("/{voucherId}/seckill-tokens")
    @RequireRole(RoleEnum.USER)
    public Result<SeckillTokenResponse> token(@PathVariable("voucherId") Long voucherId) {
        return Result.ok(seckillService.createToken(UserContext.getUserId(), voucherId));
    }

    @Operation(summary = "秒杀下单(异步)")
    @PostMapping("/{voucherId}/orders")
    @RequireRole(RoleEnum.USER)
    public ResponseEntity<Result<SeckillOrderResponse>> seckill(
            @PathVariable("voucherId") Long voucherId,
            @Parameter(description = "客户端生成的幂等键,同一次提交必须复用")
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody SeckillOrderRequest request) {
        SeckillOrderResponse response = seckillService.seckill(
                UserContext.getUserId(), idempotencyKey, voucherId, request.token());
        return ResponseEntity.accepted().body(Result.ok(response));
    }

    @Operation(summary = "我的秒杀订单(可轮询)")
    @GetMapping("/{voucherId}/my-order")
    @RequireRole(RoleEnum.USER)
    public Result<VoucherOrderResponse> myOrder(@PathVariable("voucherId") Long voucherId) {
        return Result.ok(seckillService.myOrder(UserContext.getUserId(), voucherId));
    }

    private Merchant requireMerchant() {
        Merchant merchant = merchantService.getApprovedByUserId(UserContext.getUserId());
        if (merchant == null) {
            throw new BusinessException(ErrorCode.MERCHANT_NOT_APPROVED);
        }
        return merchant;
    }
}
