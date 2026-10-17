package com.laotang.parameter.user.response;

/**
 * 当前登录用户的完整资料,包含手机号。
 */
public record MyProfileResponse(
        String id,
        String phone,
        String nickname,
        String avatar,
        String intro,
        String role,
        long followerCount,
        long followingCount,
        String createdAt
) {
}
