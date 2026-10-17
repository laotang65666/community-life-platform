package com.laotang.core.content.controller;

import com.laotang.common.annotation.RequireRole;
import com.laotang.common.context.UserContext;
import com.laotang.common.enums.RoleEnum;
import com.laotang.common.result.PageResult;
import com.laotang.common.result.Result;
import com.laotang.common.util.PageUtils;
import com.laotang.core.content.service.ICommentService;
import com.laotang.parameter.content.request.CreateCommentRequest;
import com.laotang.parameter.content.response.CommentResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 评论接口。
 */
@Tag(name = "内容社区-评论")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@RequireRole(RoleEnum.USER)
public class CommentController {

    private final ICommentService commentService;

    @Operation(summary = "评论列表")
    @GetMapping("/blog-posts/{postId}/comments")
    public Result<PageResult<CommentResponse>> list(
            @PathVariable("postId") Long postId,
            @RequestParam(value = "page", defaultValue = "1") long page,
            @RequestParam(value = "size", defaultValue = "10") long size) {
        return Result.ok(commentService.list(
                postId, PageUtils.normalizePage(page), PageUtils.normalizeSize(size)));
    }

    @Operation(summary = "发表评论 / 回复")
    @PostMapping("/blog-posts/{postId}/comments")
    public Result<CommentResponse> add(@PathVariable("postId") Long postId,
                                       @Valid @RequestBody CreateCommentRequest request) {
        return Result.ok(commentService.add(postId, UserContext.getUserId(), request));
    }

    @Operation(summary = "删除评论")
    @DeleteMapping("/comments/{commentId}")
    public Result<Void> delete(@PathVariable("commentId") Long commentId) {
        boolean admin = UserContext.getRole() == RoleEnum.ADMIN;
        commentService.delete(commentId, UserContext.getUserId(), admin);
        return Result.ok();
    }
}
