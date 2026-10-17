package com.laotang.core.stats.controller;

import com.laotang.common.annotation.RequireRole;
import com.laotang.common.enums.RoleEnum;
import com.laotang.common.result.Result;
import com.laotang.core.stats.service.StatsQueryService;
import com.laotang.parameter.stats.response.DashboardSummaryResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理后台数据看板。
 */
@Tag(name = "管理后台 - 数据看板")
@RestController
@RequestMapping("/api/v1/admin/dashboard")
@RequiredArgsConstructor
@RequireRole(RoleEnum.ADMIN)
public class AdminDashboardController {

    private final StatsQueryService statsQueryService;

    @Operation(summary = "平台数据看板")
    @GetMapping("/summary")
    public Result<DashboardSummaryResponse> summary() {
        return Result.ok(statsQueryService.dashboardSummary());
    }
}
