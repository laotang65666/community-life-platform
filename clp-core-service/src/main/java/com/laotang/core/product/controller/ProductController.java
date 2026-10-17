package com.laotang.core.product.controller;

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
import com.laotang.core.product.service.IProductService;
import com.laotang.parameter.product.request.CreateProductRequest;
import com.laotang.parameter.product.request.UpdateProductRequest;
import com.laotang.parameter.product.response.ProductBriefResponse;
import com.laotang.parameter.product.response.ProductResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 商品接口。查询公开,写操作仅限店铺所属商户。
 */
@Tag(name = "商品")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ProductController {

    private final IProductService productService;
    private final IMerchantService merchantService;

    @Operation(summary = "店铺在售商品")
    @GetMapping("/shops/{shopId}/products")
    public Result<PageResult<ProductBriefResponse>> listByShop(
            @PathVariable("shopId") Long shopId,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "page", defaultValue = "1") long page,
            @RequestParam(value = "size", defaultValue = "10") long size) {
        return Result.ok(productService.listByShop(
                shopId,
                PageUtils.normalizePage(page),
                PageUtils.normalizeSize(size),
                keyword));
    }

    @Operation(summary = "店铺全部商品(含下架,商户专用)")
    @GetMapping("/shops/{shopId}/products/manage")
    @RequireRole(RoleEnum.MERCHANT)
    public Result<PageResult<ProductBriefResponse>> listManage(
            @PathVariable("shopId") Long shopId,
            @RequestParam(value = "page", defaultValue = "1") long page,
            @RequestParam(value = "size", defaultValue = "10") long size) {
        Merchant merchant = requireMerchant();
        return Result.ok(productService.listManage(
                shopId,
                merchant.getId(),
                PageUtils.normalizePage(page),
                PageUtils.normalizeSize(size)));
    }

    @Operation(summary = "新增商品")
    @PostMapping("/shops/{shopId}/products")
    @RequireRole(RoleEnum.MERCHANT)
    public Result<ProductResponse> create(@PathVariable("shopId") Long shopId,
                                          @Valid @RequestBody CreateProductRequest request) {
        Merchant merchant = requireMerchant();
        return Result.ok(productService.create(shopId, merchant.getId(), request));
    }

    @Operation(summary = "商品详情")
    @GetMapping("/products/{productId}")
    public Result<ProductResponse> detail(@PathVariable("productId") Long productId) {
        return Result.ok(productService.getDetail(productId));
    }

    @Operation(summary = "修改商品 / 上下架")
    @PatchMapping("/products/{productId}")
    @RequireRole(RoleEnum.MERCHANT)
    public Result<Void> update(@PathVariable("productId") Long productId,
                               @Valid @RequestBody UpdateProductRequest request) {
        Merchant merchant = requireMerchant();
        productService.update(productId, merchant.getId(), request);
        return Result.ok();
    }

    @Operation(summary = "删除商品")
    @DeleteMapping("/products/{productId}")
    @RequireRole(RoleEnum.MERCHANT)
    public Result<Void> delete(@PathVariable("productId") Long productId) {
        Merchant merchant = requireMerchant();
        productService.delete(productId, merchant.getId());
        return Result.ok();
    }

    private Merchant requireMerchant() {
        Merchant merchant = merchantService.getApprovedByUserId(UserContext.getUserId());
        if (merchant == null) {
            throw new BusinessException(ErrorCode.MERCHANT_NOT_APPROVED);
        }
        return merchant;
    }
}
