package com.laotang.parameter.ai.response;

/**
 * 一次问答的返回。
 *
 * <p>用户消息与助手消息都已经落库,前端可直接用 {@code messageId} 做去重。</p>
 */
public record ChatReplyResponse(
        String conversationId,
        String messageId,
        String content,
        int tokensIn,
        int tokensOut,
        String createdAt
) {
}
