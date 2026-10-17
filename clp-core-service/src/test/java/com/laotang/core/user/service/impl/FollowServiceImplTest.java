package com.laotang.core.user.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.laotang.common.exception.BusinessException;
import com.laotang.common.exception.ErrorCode;
import com.laotang.common.result.PageResult;
import com.laotang.core.user.entity.Follow;
import com.laotang.core.user.entity.User;
import com.laotang.core.user.mapper.FollowMapper;
import com.laotang.core.user.service.IUserService;
import com.laotang.parameter.user.response.UserBriefResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FollowServiceImplTest {

    @Mock
    private FollowMapper followMapper;
    @Mock
    private IUserService userService;
    @Mock
    private StringRedisTemplate redis;
    @Mock
    private ValueOperations<String, String> valueOps;
    @InjectMocks
    private FollowServiceImpl followService;

    @BeforeEach
    void setUp() {
        lenient().when(redis.opsForValue()).thenReturn(valueOps);
    }

    @Test
    @DisplayName("不能关注自己 -> 3005")
    void follow_self() {
        BusinessException ex = catchThrowableOfType(
                () -> followService.follow(1L, 1L), BusinessException.class);

        assertThat(ex).isNotNull();
        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.CANNOT_FOLLOW_SELF);
    }

    @Test
    @DisplayName("关注不存在的用户 -> 1002")
    void follow_targetNotFound() {
        when(userService.getById(2L)).thenReturn(null);

        BusinessException ex = catchThrowableOfType(
                () -> followService.follow(1L, 2L), BusinessException.class);

        assertThat(ex).isNotNull();
        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND);
    }

    @Test
    @DisplayName("重复关注是幂等的,不抛异常")
    void follow_duplicateIsIdempotent() {
        when(userService.getById(2L)).thenReturn(new User());
        when(valueOps.get(anyString())).thenReturn("0");
        when(followMapper.insert(any(Follow.class))).thenThrow(new DuplicateKeyException("dup"));

        assertThatCode(() -> followService.follow(1L, 2L)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("取消关注调用删除并递减粉丝数")
    void unfollow() {
        when(valueOps.get(anyString())).thenReturn("1");
        when(followMapper.delete(any())).thenReturn(1);

        followService.unfollow(1L, 2L);

        verify(followMapper).delete(any());
        verify(valueOps).decrement("clp:user:followers:2");
    }

    @Test
    @DisplayName("粉丝列表返回用户名片")
    void listFollowers() {
        Follow follow = new Follow();
        follow.setUserId(5L);
        follow.setFollowUserId(1L);

        Page<Follow> page = new Page<>(1, 10);
        page.setRecords(List.of(follow));
        page.setTotal(1);

        User user = new User();
        user.setId(5L);
        user.setNickname("粉丝");
        user.setAvatar("avatar.png");

        when(followMapper.selectPage(any(), any())).thenReturn(page);
        when(userService.findByIds(List.of(5L))).thenReturn(List.of(user));

        PageResult<UserBriefResponse> result = followService.listFollowers(1L, 1, 10);

        assertThat(result.total()).isEqualTo(1);
        assertThat(result.list()).hasSize(1);
        assertThat(result.list().getFirst().id()).isEqualTo("5");
        assertThat(result.list().getFirst().nickname()).isEqualTo("粉丝");
    }
}
