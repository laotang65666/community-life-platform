package com.laotang.parameter.ai.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 创建 AI 会话。
 *
 * @param scope USER:用户端客服;MERCHANT:商户端助手
 * @param title 可选标题,为空时由首条消息自动生成
 */
public record CreateConversationRequest(

        @NotBlank(message = "scope 不能为空")
        String scope,

        @Size(max = 128, message = "标题不能超过 128 个字符")
        String title
) {
}
