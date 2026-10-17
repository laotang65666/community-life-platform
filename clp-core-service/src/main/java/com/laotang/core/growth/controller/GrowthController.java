package com.laotang.core.growth.controller;

import com.laotang.common.annotation.RequireRole;
import com.laotang.common.context.UserContext;
import com.laotang.common.enums.RoleEnum;
import com.laotang.common.result.PageResult;
import com.laotang.common.result.Result;
import com.laotang.common.util.PageUtils;
import com.laotang.core.growth.service.IGrowthService;
import com.laotang.parameter.growth.response.MonthSignResponse;
import com.laotang.parameter.growth.response.PointLogResponse;
import com.laotang.parameter.growth.response.PointsResponse;
import com.laotang.parameter.growth.response.SignInResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 签到与积分接口。
 */
@Tag(name = "签到 / 积分")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@RequireRole(RoleEnum.USER)
public class GrowthController {

    private final IGrowthService growthService;

    @Operation(summary = "今日签到")
    @PostMapping("/sign-ins")
    public Result<SignInResponse> signIn() {
        return Result.ok(growthService.signIn(UserContext.getUserId()));
    }

    @Operation(summary = "本月签到记录")
    @GetMapping("/sign-ins/current-month")
    public Result<MonthSignResponse> currentMonth() {
        return Result.ok(growthService.currentMonth(UserContext.getUserId()));
    }

    @Operation(summary = "我的积分")
    @GetMapping("/points")
    public Result<PointsResponse> points() {
        return Result.ok(growthService.points(UserContext.getUserId()));
    }

    @Operation(summary = "积分流水")
    @GetMapping("/points/logs")
    public Result<PageResult<PointLogResponse>> pointLogs(
            @RequestParam(value = "page", defaultValue = "1") long page,
            @RequestParam(value = "size", defaultValue = "10") long size) {
        return Result.ok(growthService.pointLogs(
                UserContext.getUserId(),
                PageUtils.normalizePage(page),
                PageUtils.normalizeSize(size)));
    }
}
