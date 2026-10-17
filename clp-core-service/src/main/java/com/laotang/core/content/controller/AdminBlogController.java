package com.laotang.core.content.controller;

import com.laotang.common.annotation.RequireRole;
import com.laotang.common.enums.RoleEnum;
import com.laotang.common.result.PageResult;
import com.laotang.common.result.Result;
import com.laotang.common.util.PageUtils;
import com.laotang.core.content.service.IBlogService;
import com.laotang.parameter.content.response.BlogBriefResponse;
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
 * 管理端内容审核。
 */
@Tag(name = "管理-内容审核")
@RestController
@RequestMapping("/api/v1/admin/blog-posts")
@RequiredArgsConstructor
@RequireRole(RoleEnum.ADMIN)
public class AdminBlogController {

    private final IBlogService blogService;

    @Operation(summary = "笔记审核列表")
    @GetMapping
    public Result<PageResult<BlogBriefResponse>> list(
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "page", defaultValue = "1") long page,
            @RequestParam(value = "size", defaultValue = "10") long size) {
        return Result.ok(blogService.listForAdmin(
                status, PageUtils.normalizePage(page), PageUtils.normalizeSize(size)));
    }

    @Operation(summary = "隐藏笔记")
    @PostMapping("/{postId}/hide")
    public Result<Void> hide(@PathVariable("postId") Long postId) {
        blogService.hide(postId);
        return Result.ok();
    }
}
