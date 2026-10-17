package com.laotang.parameter.user.request;

import jakarta.validation.constraints.Size;

/**
 * 修改个人资料。字段为 null 表示不修改。
 */
public record UpdateProfileRequest(

        @Size(max = 50, message = "昵称不能超过 50 个字符")
        String nickname,

        @Size(max = 500, message = "头像 URL 不能超过 500 个字符")
        String avatar,

        @Size(max = 255, message = "简介不能超过 255 个字符")
        String intro
) {
}
