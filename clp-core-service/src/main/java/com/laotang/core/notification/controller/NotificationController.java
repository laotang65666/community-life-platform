package com.laotang.core.notification.controller;

import com.laotang.common.annotation.RequireRole;
import com.laotang.common.context.UserContext;
import com.laotang.common.enums.RoleEnum;
import com.laotang.common.result.PageResult;
import com.laotang.common.result.Result;
import com.laotang.common.util.PageUtils;
import com.laotang.core.notification.service.INotificationService;
import com.laotang.parameter.notification.response.NotificationResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 通知接口。
 */
@Tag(name = "通知")
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@RequireRole(RoleEnum.USER)
public class NotificationController {

    private final INotificationService notificationService;

    @Operation(summary = "通知列表")
    @GetMapping
    public Result<PageResult<NotificationResponse>> list(
            @RequestParam(value = "page", defaultValue = "1") long page,
            @RequestParam(value = "size", defaultValue = "10") long size) {
        return Result.ok(notificationService.list(
                UserContext.getUserId(),
                PageUtils.normalizePage(page),
                PageUtils.normalizeSize(size)));
    }

    @Operation(summary = "未读数量")
    @GetMapping("/unread-count")
    public Result<Long> unreadCount() {
        return Result.ok(notificationService.unreadCount(UserContext.getUserId()));
    }

    @Operation(summary = "标记单条已读")
    @PostMapping("/{notificationId}/read")
    public Result<Void> markRead(@PathVariable("notificationId") Long notificationId) {
        notificationService.markRead(UserContext.getUserId(), notificationId);
        return Result.ok();
    }

    @Operation(summary = "全部标记已读")
    @PostMapping("/read-all")
    public Result<Void> markAllRead() {
        notificationService.markAllRead(UserContext.getUserId());
        return Result.ok();
    }
}
