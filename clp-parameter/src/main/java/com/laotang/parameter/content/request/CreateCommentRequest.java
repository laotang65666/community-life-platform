package com.laotang.parameter.content.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 发表评论或回复。
 */
public record CreateCommentRequest(

        @NotBlank(message = "评论内容不能为空")
        @Size(max = 1000, message = "评论不能超过 1000 个字符")
        String content,

        Long parentId
) {
}
