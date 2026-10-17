package com.laotang.core.auth.jwt;

import com.laotang.common.enums.RoleEnum;
import com.laotang.common.exception.BusinessException;
import com.laotang.common.exception.ErrorCode;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

class JwtTokenProviderTest {

    private static final String SECRET = "test-secret-0123456789-0123456789-0123456789";

    private final JwtTokenProvider provider = new JwtTokenProvider(new JwtProperties(
            SECRET, Duration.ofMinutes(30), Duration.ofDays(7), "test"));

    @Test
    @DisplayName("accessToken 能被解析,userId / role / type 正确")
    void accessToken_canBeParsed() {
        String token = provider.createAccessToken(1001L, RoleEnum.MERCHANT);

        Claims claims = provider.parse(token);

        assertThat(provider.getUserId(claims)).isEqualTo(1001L);
        assertThat(provider.getRole(claims)).isEqualTo(RoleEnum.MERCHANT);
        assertThat(provider.isAccessToken(claims)).isTrue();
        assertThat(provider.isRefreshToken(claims)).isFalse();
    }

    @Test
    @DisplayName("refreshToken 带 jti,类型为 REFRESH")
    void refreshToken_hasJti() {
        String token = provider.createRefreshToken(1001L, "jti-001");

        Claims claims = provider.parse(token);

        assertThat(provider.isRefreshToken(claims)).isTrue();
        assertThat(provider.getJti(claims)).isEqualTo("jti-001");
    }

    @Test
    @DisplayName("过期 token 解析抛出 2002")
    void expiredToken_throws() {
        JwtTokenProvider expiredProvider = new JwtTokenProvider(new JwtProperties(
                SECRET, Duration.ofSeconds(-1), Duration.ofDays(7), "test"));

        String token = expiredProvider.createAccessToken(1001L, RoleEnum.USER);

        BusinessException ex = catchThrowableOfType(
                () -> expiredProvider.parse(token), BusinessException.class);

        assertThat(ex).isNotNull();
        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.TOKEN_INVALID);
    }

    @Test
    @DisplayName("篡改签名解析抛出 2002")
    void tamperedToken_throws() {
        String token = provider.createAccessToken(1001L, RoleEnum.USER);
        String tampered = token.substring(0, token.length() - 4) + "abcd";

        BusinessException ex = catchThrowableOfType(
                () -> provider.parse(tampered), BusinessException.class);

        assertThat(ex).isNotNull();
        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.TOKEN_INVALID);
    }

    @Test
    @DisplayName("密钥不足 32 字节时构造失败")
    void shortSecret_failsFast() {
        IllegalStateException ex = catchThrowableOfType(
                () -> new JwtTokenProvider(new JwtProperties(
                        "too-short", Duration.ofMinutes(30), Duration.ofDays(7), "test")),
                IllegalStateException.class);

        assertThat(ex).isNotNull();
    }
}
