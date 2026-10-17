package com.laotang.core.user.controller;

import com.laotang.common.annotation.RequireRole;
import com.laotang.common.context.UserContext;
import com.laotang.common.enums.RoleEnum;
import com.laotang.common.result.Result;
import com.laotang.core.user.service.IUserService;
import com.laotang.parameter.user.request.UpdateProfileRequest;
import com.laotang.parameter.user.response.MyProfileResponse;
import com.laotang.parameter.user.response.UserProfileResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户资料接口。
 */
@Tag(name = "用户")
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@RequireRole(RoleEnum.USER)
public class UserController {

    private final IUserService userService;

    @Operation(summary = "当前用户资料")
    @GetMapping("/me")
    public Result<MyProfileResponse> me() {
        return Result.ok(userService.getMyProfile(UserContext.getUserId()));
    }

    @Operation(summary = "修改当前用户资料")
    @PatchMapping("/me")
    public Result<Void> updateProfile(@Valid @RequestBody UpdateProfileRequest request) {
        userService.updateProfile(UserContext.getUserId(), request);
        return Result.ok();
    }

    @Operation(summary = "用户公开资料")
    @GetMapping("/{userId}")
    public Result<UserProfileResponse> profile(@PathVariable("userId") Long userId) {
        return Result.ok(userService.getProfile(userId, UserContext.getUserId()));
    }
}
