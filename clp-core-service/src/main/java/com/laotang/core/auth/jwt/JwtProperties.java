package com.laotang.core.auth.jwt;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * JWT 配置,对应 application.yml 的 clp.jwt。
 */
@ConfigurationProperties(prefix = "clp.jwt")
public record JwtProperties(
        String secret,
        Duration accessTokenExpire,
        Duration refreshTokenExpire,
        String issuer
) {
}
