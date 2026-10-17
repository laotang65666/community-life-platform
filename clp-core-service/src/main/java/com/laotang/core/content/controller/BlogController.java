package com.laotang.core.content.controller;

import com.laotang.common.annotation.RequireRole;
import com.laotang.common.context.UserContext;
import com.laotang.common.enums.RoleEnum;
import com.laotang.common.result.PageResult;
import com.laotang.common.result.Result;
import com.laotang.common.util.PageUtils;
import com.laotang.core.content.service.IBlogService;
import com.laotang.parameter.content.request.CreateBlogRequest;
import com.laotang.parameter.content.response.BlogBriefResponse;
import com.laotang.parameter.content.response.BlogResponse;
import com.laotang.parameter.content.response.FeedResponse;
import com.laotang.parameter.user.response.UserBriefResponse;
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
 * 笔记接口。
 */
@Tag(name = "内容社区")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@RequireRole(RoleEnum.USER)
public class BlogController {

    private final IBlogService blogService;

    @Operation(summary = "发布笔记")
    @PostMapping("/blog-posts")
    public Result<BlogResponse> create(@Valid @RequestBody CreateBlogRequest request) {
        return Result.ok(blogService.create(UserContext.getUserId(), request));
    }

    @Operation(summary = "热门笔记")
    @GetMapping("/blog-posts/hot")
    public Result<PageResult<BlogBriefResponse>> hot(
            @RequestParam(value = "page", defaultValue = "1") long page,
            @RequestParam(value = "size", defaultValue = "10") long size) {
        return Result.ok(blogService.listHot(
                PageUtils.normalizePage(page), PageUtils.normalizeSize(size)));
    }

    @Operation(summary = "关注流")
    @GetMapping("/blog-posts/feed")
    public Result<FeedResponse> feed(
            @RequestParam(value = "lastId", required = false) Long lastId,
            @RequestParam(value = "offset", defaultValue = "0") int offset,
            @RequestParam(value = "size", defaultValue = "10") int size) {
        int normalizedOffset = Math.max(offset, 0);
        int normalizedSize = (int) PageUtils.normalizeSize(size);
        return Result.ok(blogService.feed(
                UserContext.getUserId(), lastId, normalizedOffset, normalizedSize));
    }

    @Operation(summary = "笔记详情")
    @GetMapping("/blog-posts/{postId}")
    public Result<BlogResponse> detail(@PathVariable("postId") Long postId) {
        return Result.ok(blogService.getDetail(postId, UserContext.getUserId()));
    }

    @Operation(summary = "删除笔记")
    @DeleteMapping("/blog-posts/{postId}")
    public Result<Void> delete(@PathVariable("postId") Long postId) {
        blogService.delete(postId, UserContext.getUserId());
        return Result.ok();
    }

    @Operation(summary = "某个用户的笔记")
    @GetMapping("/users/{userId}/blog-posts")
    public Result<PageResult<BlogBriefResponse>> byAuthor(
            @PathVariable("userId") Long userId,
            @RequestParam(value = "page", defaultValue = "1") long page,
            @RequestParam(value = "size", defaultValue = "10") long size) {
        return Result.ok(blogService.listByAuthor(
                userId, PageUtils.normalizePage(page), PageUtils.normalizeSize(size)));
    }

    @Operation(summary = "点赞")
    @PostMapping("/blog-posts/{postId}/likes")
    public Result<Void> like(@PathVariable("postId") Long postId) {
        blogService.like(postId, UserContext.getUserId());
        return Result.ok();
    }

    @Operation(summary = "取消点赞")
    @DeleteMapping("/blog-posts/{postId}/likes")
    public Result<Void> unlike(@PathVariable("postId") Long postId) {
        blogService.unlike(postId, UserContext.getUserId());
        return Result.ok();
    }

    @Operation(summary = "点赞用户列表")
    @GetMapping("/blog-posts/{postId}/likes")
    public Result<PageResult<UserBriefResponse>> likes(
            @PathVariable("postId") Long postId,
            @RequestParam(value = "page", defaultValue = "1") long page,
            @RequestParam(value = "size", defaultValue = "10") long size) {
        return Result.ok(blogService.listLikes(
                postId, PageUtils.normalizePage(page), PageUtils.normalizeSize(size)));
    }

    @Operation(summary = "收藏")
    @PostMapping("/blog-posts/{postId}/favorites")
    public Result<Void> favorite(@PathVariable("postId") Long postId) {
        blogService.favorite(postId, UserContext.getUserId());
        return Result.ok();
    }

    @Operation(summary = "取消收藏")
    @DeleteMapping("/blog-posts/{postId}/favorites")
    public Result<Void> unfavorite(@PathVariable("postId") Long postId) {
        blogService.unfavorite(postId, UserContext.getUserId());
        return Result.ok();
    }

    @Operation(summary = "我的收藏")
    @GetMapping("/users/me/favorites")
    public Result<PageResult<BlogBriefResponse>> myFavorites(
            @RequestParam(value = "page", defaultValue = "1") long page,
            @RequestParam(value = "size", defaultValue = "10") long size) {
        return Result.ok(blogService.listFavorites(
                UserContext.getUserId(), PageUtils.normalizePage(page), PageUtils.normalizeSize(size)));
    }
}
