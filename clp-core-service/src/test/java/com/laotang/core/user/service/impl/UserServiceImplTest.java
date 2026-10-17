package com.laotang.core.user.service.impl;

import com.laotang.common.exception.BusinessException;
import com.laotang.common.exception.ErrorCode;
import com.laotang.core.user.entity.User;
import com.laotang.core.user.mapper.FollowMapper;
import com.laotang.core.user.mapper.UserMapper;
import com.laotang.parameter.user.response.UserProfileResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserMapper userMapper;
    @Mock
    private FollowMapper followMapper;
    @InjectMocks
    private UserServiceImpl userService;

    @BeforeEach
    void setUp() {
        // ServiceImpl 的 baseMapper 是父类字段,@InjectMocks 注不进去
        ReflectionTestUtils.setField(userService, "baseMapper", userMapper);
    }

    @Test
    @DisplayName("查看不存在的用户 -> 1002")
    void getProfile_userNotFound() {
        when(userMapper.selectById(99L)).thenReturn(null);

        BusinessException ex = catchThrowableOfType(
                () -> userService.getProfile(99L, 1L), BusinessException.class);

        assertThat(ex).isNotNull();
        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND);
    }

    @Test
    @DisplayName("查看资料返回粉丝数、关注数和是否已关注")
    void getProfile_success() {
        User user = new User();
        user.setId(1L);
        user.setNickname("小明");
        user.setRole("USER");

        when(userMapper.selectById(1L)).thenReturn(user);
        when(followMapper.selectCount(any())).thenReturn(2L, 5L, 1L);

        UserProfileResponse response = userService.getProfile(1L, 2L);

        assertThat(response.id()).isEqualTo("1");
        assertThat(response.nickname()).isEqualTo("小明");
        assertThat(response.followerCount()).isEqualTo(2);
        assertThat(response.followingCount()).isEqualTo(5);
        assertThat(response.following()).isTrue();
    }

    @Test
    @DisplayName("看自己时 following 恒为 false,且只查两次计数")
    void getProfile_self_neverFollowing() {
        User user = new User();
        user.setId(1L);
        user.setNickname("小明");
        user.setRole("USER");

        when(userMapper.selectById(1L)).thenReturn(user);
        when(followMapper.selectCount(any())).thenReturn(0L, 0L);

        UserProfileResponse response = userService.getProfile(1L, 1L);

        assertThat(response.following()).isFalse();
    }
}
