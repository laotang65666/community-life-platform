package com.laotang.parameter.ai.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 发送一条消息给 AI 客服。
 */
public record SendMessageRequest(

        @NotBlank(message = "消息内容不能为空")
        @Size(max = 2000, message = "单条消息不能超过 2000 个字符")
        String content
) {
}
