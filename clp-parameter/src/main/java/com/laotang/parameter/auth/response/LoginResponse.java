package com.laotang.parameter.auth.response;

/**
 * 登录 / 刷新成功响应。
 *
 * @param accessToken  accessToken,30 分钟有效
 * @param refreshToken refreshToken,7 天有效
 * @param expiresIn    accessToken 剩余秒数
 * @param role         当前角色
 */
public record LoginResponse(
        String accessToken,
        String refreshToken,
        long expiresIn,
        String role
) {
}
