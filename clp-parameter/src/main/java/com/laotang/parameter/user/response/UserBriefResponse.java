package com.laotang.parameter.user.response;

/**
 * 用户简要信息,用于粉丝 / 关注列表。
 */
public record UserBriefResponse(
        String id,
        String nickname,
        String avatar
) {
}
