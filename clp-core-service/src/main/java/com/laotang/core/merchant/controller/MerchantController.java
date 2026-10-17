package com.laotang.core.merchant.controller;

import com.laotang.common.annotation.RequireRole;
import com.laotang.common.context.UserContext;
import com.laotang.common.enums.RoleEnum;
import com.laotang.common.result.Result;
import com.laotang.core.merchant.service.IMerchantService;
import com.laotang.parameter.merchant.request.ApplyMerchantRequest;
import com.laotang.parameter.merchant.request.UpdateMerchantRequest;
import com.laotang.parameter.merchant.response.MerchantResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 商户入驻接口。
 */
@Tag(name = "商户")
@RestController
@RequestMapping("/api/v1/merchants")
@RequiredArgsConstructor
@RequireRole(RoleEnum.USER)
public class MerchantController {

    private final IMerchantService merchantService;

    @Operation(summary = "提交入驻申请")
    @PostMapping
    public Result<MerchantResponse> apply(@Valid @RequestBody ApplyMerchantRequest request) {
        return Result.ok(merchantService.apply(UserContext.getUserId(), request));
    }

    @Operation(summary = "我的商户信息")
    @GetMapping("/me")
    public Result<MerchantResponse> me() {
        return Result.ok(merchantService.getMyMerchant(UserContext.getUserId()));
    }

    @Operation(summary = "修改入驻信息")
    @PatchMapping("/me")
    public Result<Void> update(@Valid @RequestBody UpdateMerchantRequest request) {
        merchantService.updateMyMerchant(UserContext.getUserId(), request);
        return Result.ok();
    }
}
