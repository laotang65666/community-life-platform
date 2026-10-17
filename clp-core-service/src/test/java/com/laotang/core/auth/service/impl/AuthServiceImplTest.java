package com.laotang.core.auth.service.impl;

import com.laotang.common.exception.BusinessException;
import com.laotang.common.exception.ErrorCode;
import com.laotang.core.auth.jwt.JwtProperties;
import com.laotang.core.auth.jwt.JwtTokenProvider;
import com.laotang.core.auth.support.SmsSender;
import com.laotang.core.user.entity.User;
import com.laotang.core.user.service.IUserService;
import com.laotang.parameter.auth.response.LoginResponse;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private StringRedisTemplate redis;
    @Mock
    private ValueOperations<String, String> valueOps;
    @Mock
    private IUserService userService;
    @Mock
    private JwtTokenProvider jwtTokenProvider;
    @Mock
    private SmsSender smsSender;

    private final JwtProperties properties = new JwtProperties(
            "test-secret-0123456789-0123456789-0123456789",
            Duration.ofMinutes(30),
            Duration.ofDays(7),
            "test");

    private AuthServiceImpl service;

    @BeforeEach
    void setUp() {
        lenient().when(redis.opsForValue()).thenReturn(valueOps);
        service = new AuthServiceImpl(redis, userService, jwtTokenProvider, properties, smsSender);
    }

    @Test
    @DisplayName("60 秒内重复发送验证码 -> 3003")
    void sendSmsCode_tooFrequent() {
        when(valueOps.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(false);

        BusinessException ex = catchThrowableOfType(
                () -> service.sendSmsCode("13800138000"), BusinessException.class);

        assertThat(ex).isNotNull();
        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.SMS_TOO_FREQUENT);
    }

    @Test
    @DisplayName("验证码错误 -> 3002,并记录失败次数")
    void login_wrongCode() {
        when(valueOps.get(anyString())).thenReturn("111111");

        BusinessException ex = catchThrowableOfType(
                () -> service.login("13800138000", "222222"), BusinessException.class);

        assertThat(ex).isNotNull();
        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.SMS_CODE_INVALID);
        verify(valueOps).increment(anyString());
    }

    @Test
    @DisplayName("验证码正确且用户不存在 -> 自动注册并返回令牌")
    void login_newUser() {
        when(valueOps.get(anyString())).thenReturn("123456");
        when(userService.findByPhone("13800138000")).thenReturn(null);

        User created = new User();
        created.setId(1001L);
        created.setPhone("13800138000");
        created.setRole("USER");
        created.setStatus("ACTIVE");
        when(userService.createByPhone("13800138000")).thenReturn(created);

        when(jwtTokenProvider.createAccessToken(1001L, com.laotang.common.enums.RoleEnum.USER))
                .thenReturn("access-token");
        when(jwtTokenProvider.createRefreshToken(any(), anyString())).thenReturn("refresh-token");

        LoginResponse response = service.login("13800138000", "123456");

        assertThat(response.accessToken()).isEqualTo("access-token");
        assertThat(response.refreshToken()).isEqualTo("refresh-token");
        assertThat(response.role()).isEqualTo("USER");
        verify(userService).updateLastLogin(1001L);
    }

    @Test
    @DisplayName("refresh 的 jti 不存在 -> 2002")
    void refresh_jtiMissing() {
        Claims claims = org.mockito.Mockito.mock(Claims.class);
        when(jwtTokenProvider.parse("refresh-token")).thenReturn(claims);
        when(jwtTokenProvider.isRefreshToken(claims)).thenReturn(true);
        when(jwtTokenProvider.getUserId(claims)).thenReturn(1001L);
        when(jwtTokenProvider.getJti(claims)).thenReturn("jti-001");
        when(redis.hasKey(anyString())).thenReturn(false);

        BusinessException ex = catchThrowableOfType(
                () -> service.refresh("refresh-token"), BusinessException.class);

        assertThat(ex).isNotNull();
        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.TOKEN_INVALID);
    }

    @Test
    @DisplayName("logout 删除 refresh key")
    void logout_deletesRefreshKey() {
        Claims claims = org.mockito.Mockito.mock(Claims.class);
        when(jwtTokenProvider.parse("refresh-token")).thenReturn(claims);
        when(jwtTokenProvider.isRefreshToken(claims)).thenReturn(true);
        when(jwtTokenProvider.getUserId(claims)).thenReturn(1001L);
        when(jwtTokenProvider.getJti(claims)).thenReturn("jti-001");

        service.logout("refresh-token");

        verify(redis).delete("clp:auth:refresh:1001:jti-001");
    }
}
