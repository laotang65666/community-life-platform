package com.laotang.core.content.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.laotang.common.exception.BusinessException;
import com.laotang.common.exception.ErrorCode;
import com.laotang.common.result.PageResult;
import com.laotang.core.content.converter.BlogConverter;
import com.laotang.core.content.entity.Blog;
import com.laotang.core.content.entity.BlogComment;
import com.laotang.core.content.mapper.BlogCommentMapper;
import com.laotang.core.content.mapper.BlogMapper;
import com.laotang.core.content.service.ICommentService;
import com.laotang.core.user.entity.User;
import com.laotang.core.user.service.IUserService;
import com.laotang.parameter.content.request.CreateCommentRequest;
import com.laotang.parameter.content.response.CommentResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 评论服务实现。
 */
@Service
@RequiredArgsConstructor
public class CommentServiceImpl implements ICommentService {

    private static final String PUBLISHED = "PUBLISHED";
    private static final String VISIBLE = "VISIBLE";
    private static final String HIDDEN = "HIDDEN";

    private final BlogCommentMapper commentMapper;
    private final BlogMapper blogMapper;
    private final IUserService userService;

    @Override
    public PageResult<CommentResponse> list(Long blogId, long page, long size) {
        Page<BlogComment> result = commentMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<BlogComment>()
                        .eq(BlogComment::getBlogId, blogId)
                        .eq(BlogComment::getStatus, VISIBLE)
                        .orderByAsc(BlogComment::getCreatedAt));

        List<Long> userIds = result.getRecords().stream()
                .map(BlogComment::getUserId)
                .distinct()
                .toList();
        Map<Long, User> users = userService.findByIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, user -> user));

        List<CommentResponse> list = result.getRecords().stream()
                .map(comment -> BlogConverter.toComment(comment, users.get(comment.getUserId())))
                .toList();

        return PageResult.of(list, result.getCurrent(), result.getSize(), result.getTotal());
    }

    @Override
    public CommentResponse add(Long blogId, Long userId, CreateCommentRequest request) {
        Blog blog = blogMapper.selectById(blogId);
        if (blog == null) {
            throw new BusinessException(ErrorCode.BLOG_NOT_FOUND);
        }
        if (!PUBLISHED.equals(blog.getStatus())) {
            throw new BusinessException(ErrorCode.BLOG_HIDDEN);
        }

        Long parentId = request.parentId();
        Long replyToUserId = null;
        if (parentId != null) {
            BlogComment parent = commentMapper.selectById(parentId);
            if (parent == null || !parent.getBlogId().equals(blogId)) {
                throw new BusinessException(ErrorCode.PARAM_INVALID, "父评论不存在");
            }
            replyToUserId = parent.getUserId();
        }

        BlogComment comment = new BlogComment();
        comment.setBlogId(blogId);
        comment.setUserId(userId);
        comment.setParentId(parentId);
        comment.setReplyToUserId(replyToUserId);
        comment.setContent(request.content());
        comment.setStatus(VISIBLE);
        commentMapper.insert(comment);

        blogMapper.incrementCommentCount(blogId);

        return BlogConverter.toComment(comment, userService.getById(userId));
    }

    @Override
    public void delete(Long commentId, Long userId, boolean admin) {
        BlogComment comment = commentMapper.selectById(commentId);
        if (comment == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "评论不存在");
        }
        if (!admin && !comment.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.CONTENT_NOT_OWNER);
        }

        if (VISIBLE.equals(comment.getStatus())) {
            commentMapper.update(null, new LambdaUpdateWrapper<BlogComment>()
                    .eq(BlogComment::getId, commentId)
                    .set(BlogComment::getStatus, HIDDEN));
            blogMapper.decrementCommentCount(comment.getBlogId());
        }
        commentMapper.deleteById(commentId);
    }
}
