package com.laotang.core.content.service;

import com.laotang.common.result.PageResult;
import com.laotang.parameter.content.request.CreateCommentRequest;
import com.laotang.parameter.content.response.CommentResponse;

/**
 * 评论服务。
 */
public interface ICommentService {

    PageResult<CommentResponse> list(Long blogId, long page, long size);

    CommentResponse add(Long blogId, Long userId, CreateCommentRequest request);

    void delete(Long commentId, Long userId, boolean admin);
}
