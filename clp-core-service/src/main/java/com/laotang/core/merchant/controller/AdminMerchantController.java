package com.laotang.core.merchant.controller;

import com.laotang.common.annotation.RequireRole;
import com.laotang.common.context.UserContext;
import com.laotang.common.enums.RoleEnum;
import com.laotang.common.result.PageResult;
import com.laotang.common.result.Result;
import com.laotang.common.util.PageUtils;
import com.laotang.core.merchant.service.IMerchantService;
import com.laotang.parameter.merchant.request.ApproveMerchantRequest;
import com.laotang.parameter.merchant.request.RejectMerchantRequest;
import com.laotang.parameter.merchant.response.MerchantResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端商户审核。
 */
@Tag(name = "管理-商户审核")
@RestController
@RequestMapping("/api/v1/admin/merchants")
@RequiredArgsConstructor
@RequireRole(RoleEnum.ADMIN)
public class AdminMerchantController {

    private final IMerchantService merchantService;

    @Operation(summary = "商户申请列表")
    @GetMapping
    public Result<PageResult<MerchantResponse>> list(
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "page", defaultValue = "1") long page,
            @RequestParam(value = "size", defaultValue = "10") long size) {
        return Result.ok(merchantService.listApplications(
                status,
                PageUtils.normalizePage(page),
                PageUtils.normalizeSize(size)));
    }

    @Operation(summary = "通过入驻申请")
    @PostMapping("/{merchantId}/approve")
    public Result<Void> approve(@PathVariable("merchantId") Long merchantId,
                                @Valid @RequestBody ApproveMerchantRequest request) {
        merchantService.approve(merchantId, UserContext.getUserId(), request.typeId());
        return Result.ok();
    }

    @Operation(summary = "驳回入驻申请")
    @PostMapping("/{merchantId}/reject")
    public Result<Void> reject(@PathVariable("merchantId") Long merchantId,
                               @Valid @RequestBody RejectMerchantRequest request) {
        merchantService.reject(merchantId, UserContext.getUserId(), request.reason());
        return Result.ok();
    }
}
