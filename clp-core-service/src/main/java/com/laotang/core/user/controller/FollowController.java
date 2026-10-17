package com.laotang.core.user.controller;

import com.laotang.common.annotation.RequireRole;
import com.laotang.common.context.UserContext;
import com.laotang.common.enums.RoleEnum;
import com.laotang.common.result.PageResult;
import com.laotang.common.result.Result;
import com.laotang.core.user.service.IFollowService;
import com.laotang.parameter.user.response.UserBriefResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 关注关系接口。
 */
@Tag(name = "关注")
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@RequireRole(RoleEnum.USER)
public class FollowController {

    private static final long MAX_PAGE_SIZE = 100;

    private final IFollowService followService;

    @Operation(summary = "关注用户")
    @PostMapping("/{userId}/follow")
    public Result<Void> follow(@PathVariable("userId") Long targetUserId) {
        followService.follow(UserContext.getUserId(), targetUserId);
        return Result.ok();
    }

    @Operation(summary = "取消关注")
    @DeleteMapping("/{userId}/follow")
    public Result<Void> unfollow(@PathVariable("userId") Long targetUserId) {
        followService.unfollow(UserContext.getUserId(), targetUserId);
        return Result.ok();
    }

    @Operation(summary = "粉丝列表")
    @GetMapping("/{userId}/followers")
    public Result<PageResult<UserBriefResponse>> followers(
            @PathVariable("userId") Long userId,
            @RequestParam(value = "page", defaultValue = "1") long page,
            @RequestParam(value = "size", defaultValue = "10") long size) {
        return Result.ok(followService.listFollowers(userId, normalizePage(page), normalizeSize(size)));
    }

    @Operation(summary = "关注列表")
    @GetMapping("/{userId}/followees")
    public Result<PageResult<UserBriefResponse>> followees(
            @PathVariable("userId") Long userId,
            @RequestParam(value = "page", defaultValue = "1") long page,
            @RequestParam(value = "size", defaultValue = "10") long size) {
        return Result.ok(followService.listFollowing(userId, normalizePage(page), normalizeSize(size)));
    }

    private static long normalizePage(long page) {
        return Math.max(page, 1);
    }

    private static long normalizeSize(long size) {
        return Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
    }
}
