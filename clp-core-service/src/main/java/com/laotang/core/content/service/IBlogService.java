package com.laotang.core.content.service;

import com.laotang.common.result.PageResult;
import com.laotang.parameter.content.request.CreateBlogRequest;
import com.laotang.parameter.content.response.BlogBriefResponse;
import com.laotang.parameter.content.response.BlogResponse;
import com.laotang.parameter.content.response.FeedResponse;
import com.laotang.parameter.user.response.UserBriefResponse;

/**
 * 笔记服务。
 */
public interface IBlogService {

    BlogResponse create(Long userId, CreateBlogRequest request);

    BlogResponse getDetail(Long blogId, Long currentUserId);

    void delete(Long blogId, Long userId);

    PageResult<BlogBriefResponse> listHot(long page, long size);

    PageResult<BlogBriefResponse> listByAuthor(Long authorId, long page, long size);

    FeedResponse feed(Long userId, Long lastId, int offset, int size);

    void like(Long blogId, Long userId);

    void unlike(Long blogId, Long userId);

    PageResult<UserBriefResponse> listLikes(Long blogId, long page, long size);

    void favorite(Long blogId, Long userId);

    void unfavorite(Long blogId, Long userId);

    PageResult<BlogBriefResponse> listFavorites(Long userId, long page, long size);

    PageResult<BlogBriefResponse> listForAdmin(String status, long page, long size);

    void hide(Long blogId);
}
