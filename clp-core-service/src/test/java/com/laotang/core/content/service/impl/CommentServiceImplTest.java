package com.laotang.core.content.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.laotang.common.exception.BusinessException;
import com.laotang.common.exception.ErrorCode;
import com.laotang.core.content.entity.Blog;
import com.laotang.core.content.entity.BlogComment;
import com.laotang.core.content.mapper.BlogCommentMapper;
import com.laotang.core.content.mapper.BlogMapper;
import com.laotang.core.user.entity.User;
import com.laotang.core.user.service.IUserService;
import com.laotang.parameter.content.request.CreateCommentRequest;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommentServiceImplTest {

    @Mock
    private BlogCommentMapper commentMapper;
    @Mock
    private BlogMapper blogMapper;
    @Mock
    private IUserService userService;
    @InjectMocks
    private CommentServiceImpl commentService;

    @BeforeAll
    static void initTableInfo() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, BlogComment.class);
    }

    private Blog blog() {
        Blog blog = new Blog();
        blog.setId(100L);
        blog.setUserId(1L);
        blog.setStatus("PUBLISHED");
        return blog;
    }

    private User user() {
        User user = new User();
        user.setId(1L);
        user.setNickname("评论者");
        return user;
    }

    @Test
    @DisplayName("父评论不属于该笔记 -> 1001")
    void add_parentNotBelongToBlog() {
        when(blogMapper.selectById(100L)).thenReturn(blog());
        BlogComment parent = new BlogComment();
        parent.setId(9L);
        parent.setBlogId(200L);
        when(commentMapper.selectById(9L)).thenReturn(parent);

        BusinessException ex = catchThrowableOfType(
                () -> commentService.add(100L, 1L, new CreateCommentRequest("回复", 9L)),
                BusinessException.class);

        assertThat(ex).isNotNull();
        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.PARAM_INVALID);
    }

    @Test
    @DisplayName("发表评论成功并增加评论数")
    void add_success() {
        when(blogMapper.selectById(100L)).thenReturn(blog());
        when(commentMapper.insert(any(BlogComment.class))).thenAnswer(inv -> {
            BlogComment comment = inv.getArgument(0);
            comment.setId(500L);
            return 1;
        });
        when(userService.getById(1L)).thenReturn(user());

        var response = commentService.add(100L, 1L, new CreateCommentRequest("说得好", null));

        assertThat(response.id()).isEqualTo("500");
        assertThat(response.content()).isEqualTo("说得好");
        verify(blogMapper).incrementCommentCount(100L);
    }

    @Test
    @DisplayName("非作者非管理员不能删除评论 -> 8003")
    void delete_notOwner() {
        BlogComment comment = new BlogComment();
        comment.setId(500L);
        comment.setBlogId(100L);
        comment.setUserId(1L);
        comment.setStatus("VISIBLE");
        when(commentMapper.selectById(500L)).thenReturn(comment);

        BusinessException ex = catchThrowableOfType(
                () -> commentService.delete(500L, 2L, false), BusinessException.class);

        assertThat(ex).isNotNull();
        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.CONTENT_NOT_OWNER);
    }

    @Test
    @DisplayName("管理员可以删除任意评论")
    void delete_admin() {
        BlogComment comment = new BlogComment();
        comment.setId(500L);
        comment.setBlogId(100L);
        comment.setUserId(1L);
        comment.setStatus("VISIBLE");
        when(commentMapper.selectById(500L)).thenReturn(comment);

        commentService.delete(500L, 999L, true);

        verify(commentMapper).update(any(), any());
        verify(blogMapper).decrementCommentCount(100L);
    }
}
