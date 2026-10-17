package com.laotang.core.shop.controller;

import com.laotang.common.result.Result;
import com.laotang.core.shop.service.IShopService;
import com.laotang.parameter.shop.response.ShopTypeResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 店铺分类接口。
 */
@Tag(name = "店铺分类")
@RestController
@RequestMapping("/api/v1/shop-types")
@RequiredArgsConstructor
public class ShopTypeController {

    private final IShopService shopService;

    @Operation(summary = "店铺分类列表")
    @GetMapping
    public Result<List<ShopTypeResponse>> list() {
        return Result.ok(shopService.listTypes());
    }
}
