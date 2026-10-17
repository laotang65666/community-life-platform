package com.laotang.parameter.ai.response;

/**
 * AI 会话。
 */
public record ConversationResponse(
        String id,
        String scope,
        String title,
        String lastMessageAt,
        String createdAt
) {
}
