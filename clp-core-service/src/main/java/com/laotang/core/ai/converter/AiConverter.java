package com.laotang.core.ai.converter;

import com.laotang.common.util.DateTimeUtils;
import com.laotang.core.ai.entity.AiConversation;
import com.laotang.core.ai.entity.AiMessage;
import com.laotang.parameter.ai.response.ConversationResponse;
import com.laotang.parameter.ai.response.MessageResponse;

/**
 * AI 实体到 DTO 的转换。
 */
public final class AiConverter {

    private AiConverter() {
    }

    public static ConversationResponse toResponse(AiConversation conversation) {
        return new ConversationResponse(
                String.valueOf(conversation.getId()),
                conversation.getScope(),
                conversation.getTitle(),
                DateTimeUtils.toIso(conversation.getLastMessageAt()),
                DateTimeUtils.toIso(conversation.getCreatedAt()));
    }

    public static MessageResponse toResponse(AiMessage message) {
        return new MessageResponse(
                String.valueOf(message.getId()),
                message.getRole(),
                message.getContent(),
                message.getTokensIn() == null ? 0 : message.getTokensIn(),
                message.getTokensOut() == null ? 0 : message.getTokensOut(),
                DateTimeUtils.toIso(message.getCreatedAt()));
    }
}
