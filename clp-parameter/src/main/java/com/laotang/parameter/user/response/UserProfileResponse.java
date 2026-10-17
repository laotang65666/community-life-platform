package com.laotang.parameter.user.response;

/**
 * 用户公开资料。不包含手机号等敏感信息。
 */
public record UserProfileResponse(
        String id,
        String nickname,
        String avatar,
        String intro,
        String role,
        long followerCount,
        long followingCount,
        boolean following,
        String createdAt
) {
}
