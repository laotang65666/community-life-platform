package com.laotang.parameter.marketing.response;

/**
 * 秒杀令牌。
 */
public record SeckillTokenResponse(String token, long expiresIn) {
}
