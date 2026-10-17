package com.laotang.parameter.ai.response;

/**
 * AI 消息。
 */
public record MessageResponse(
        String id,
        String role,
        String content,
        int tokensIn,
        int tokensOut,
        String createdAt
) {
}
