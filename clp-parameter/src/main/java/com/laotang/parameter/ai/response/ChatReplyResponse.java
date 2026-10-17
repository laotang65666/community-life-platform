package com.laotang.parameter.ai.response;

import java.util.List;

/**
 * 一次问答的返回。
 *
 * <p>用户消息与助手消息都已经落库,前端可直接用 {@code messageId} 做去重。
 * {@code sources} 是 RAG 命中的知识来源,没有命中时为空数组。</p>
 */
public record ChatReplyResponse(
        String conversationId,
        String messageId,
        String content,
        int tokensIn,
        int tokensOut,
        String createdAt,
        List<SourceResponse> sources
) {
}
