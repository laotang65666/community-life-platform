package com.laotang.parameter.content.response;

/**
 * 评论。
 */
public record CommentResponse(
        String id,
        String blogId,
        String userId,
        String nickname,
        String avatar,
        String parentId,
        String replyToUserId,
        String content,
        String createdAt
) {
}
