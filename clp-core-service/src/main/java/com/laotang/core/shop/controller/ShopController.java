package com.laotang.core.shop.controller;

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
import com.laotang.core.shop.service.IShopService;
import com.laotang.parameter.shop.request.UpdateShopRequest;
import com.laotang.parameter.shop.response.ShopBriefResponse;
import com.laotang.parameter.shop.response.ShopResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 店铺接口。查询公开,修改仅限店铺所属商户。
 */
@Tag(name = "店铺")
@RestController
@RequestMapping("/api/v1/shops")
@RequiredArgsConstructor
public class ShopController {

    private final IShopService shopService;
    private final IMerchantService merchantService;

    @Operation(summary = "店铺列表")
    @GetMapping
    public Result<PageResult<ShopBriefResponse>> list(
            @RequestParam(value = "typeId", required = false) Long typeId,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "page", defaultValue = "1") long page,
            @RequestParam(value = "size", defaultValue = "10") long size) {
        return Result.ok(shopService.list(
                PageUtils.normalizePage(page),
                PageUtils.normalizeSize(size),
                typeId,
                keyword));
    }

    @Operation(summary = "店铺详情")
    @GetMapping("/{shopId}")
    public Result<ShopResponse> detail(@PathVariable("shopId") Long shopId) {
        return Result.ok(shopService.getDetail(shopId));
    }

    @Operation(summary = "修改店铺")
    @PatchMapping("/{shopId}")
    @RequireRole(RoleEnum.MERCHANT)
    public Result<Void> update(@PathVariable("shopId") Long shopId,
                               @Valid @RequestBody UpdateShopRequest request) {
        Merchant merchant = merchantService.getApprovedByUserId(UserContext.getUserId());
        if (merchant == null) {
            throw new BusinessException(ErrorCode.MERCHANT_NOT_APPROVED);
        }
        shopService.update(shopId, merchant.getId(), request);
        return Result.ok();
    }
}
