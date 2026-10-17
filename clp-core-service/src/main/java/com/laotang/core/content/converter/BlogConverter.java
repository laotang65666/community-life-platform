package com.laotang.core.content.converter;

import com.laotang.common.util.DateTimeUtils;
import com.laotang.core.content.entity.Blog;
import com.laotang.core.content.entity.BlogComment;
import com.laotang.core.user.entity.User;
import com.laotang.parameter.content.response.BlogBriefResponse;
import com.laotang.parameter.content.response.BlogResponse;
import com.laotang.parameter.content.response.CommentResponse;

import java.util.List;

/**
 * Blog / BlogComment 实体到 DTO 的转换。
 */
public final class BlogConverter {

    private BlogConverter() {
    }

    public static BlogResponse toResponse(Blog blog, User author, boolean liked, boolean favorited) {
        return new BlogResponse(
                String.valueOf(blog.getId()),
                String.valueOf(blog.getUserId()),
                author == null ? "" : author.getNickname(),
                author == null ? "" : author.getAvatar(),
                blog.getShopId() == null ? null : String.valueOf(blog.getShopId()),
                blog.getTitle(),
                blog.getContent(),
                blog.getImages() == null ? List.of() : List.copyOf(blog.getImages()),
                blog.getLikedCount() == null ? 0 : blog.getLikedCount(),
                blog.getCommentCount() == null ? 0 : blog.getCommentCount(),
                liked,
                favorited,
                DateTimeUtils.toIso(blog.getCreatedAt()));
    }

    public static BlogBriefResponse toBrief(Blog blog, User author) {
        return new BlogBriefResponse(
                String.valueOf(blog.getId()),
                String.valueOf(blog.getUserId()),
                author == null ? "" : author.getNickname(),
                author == null ? "" : author.getAvatar(),
                blog.getTitle(),
                blog.getImages() == null ? List.of() : List.copyOf(blog.getImages()),
                blog.getLikedCount() == null ? 0 : blog.getLikedCount(),
                blog.getCommentCount() == null ? 0 : blog.getCommentCount(),
                DateTimeUtils.toIso(blog.getCreatedAt()));
    }

    public static CommentResponse toComment(BlogComment comment, User user) {
        return new CommentResponse(
                String.valueOf(comment.getId()),
                String.valueOf(comment.getBlogId()),
                String.valueOf(comment.getUserId()),
                user == null ? "" : user.getNickname(),
                user == null ? "" : user.getAvatar(),
                comment.getParentId() == null ? null : String.valueOf(comment.getParentId()),
                comment.getReplyToUserId() == null ? null : String.valueOf(comment.getReplyToUserId()),
                comment.getContent(),
                DateTimeUtils.toIso(comment.getCreatedAt()));
    }
}
