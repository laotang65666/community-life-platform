package com.laotang.core.ai.service;

import com.laotang.common.enums.RoleEnum;
import com.laotang.common.result.PageResult;
import com.laotang.core.ai.enums.AiScope;
import com.laotang.parameter.ai.response.ChatReplyResponse;
import com.laotang.parameter.ai.response.ConversationResponse;
import com.laotang.parameter.ai.response.MessageResponse;

/**
 * AI 客服会话服务。
 */
public interface IAiChatService {

    ConversationResponse create(Long userId, RoleEnum role, AiScope scope, String title);

    PageResult<ConversationResponse> listConversations(
            Long userId, AiScope scope, long page, long size);

    PageResult<MessageResponse> listMessages(
            Long userId, Long conversationId, long page, long size);

    ChatReplyResponse chat(Long userId, Long conversationId, String content);

    void delete(Long userId, Long conversationId);
}
