package com.laotang.core.ai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.laotang.common.constant.RedisKey;
import com.laotang.common.enums.RoleEnum;
import com.laotang.common.exception.BusinessException;
import com.laotang.common.exception.ErrorCode;
import com.laotang.common.result.PageResult;
import com.laotang.common.util.DateTimeUtils;
import com.laotang.core.ai.config.AiChatProperties;
import com.laotang.core.ai.converter.AiConverter;
import com.laotang.core.ai.entity.AiConversation;
import com.laotang.core.ai.entity.AiMessage;
import com.laotang.core.ai.enums.AiMessageRole;
import com.laotang.core.ai.enums.AiScope;
import com.laotang.core.ai.gateway.AiChatCommand;
import com.laotang.core.ai.gateway.AiChatGateway;
import com.laotang.core.ai.gateway.ChatResult;
import com.laotang.core.ai.gateway.ChatTurn;
import com.laotang.core.ai.tool.AiToolFactory;
import com.laotang.core.ai.tool.AiToolRecorder;
import com.laotang.core.ai.mapper.AiConversationMapper;
import com.laotang.core.ai.mapper.AiMessageMapper;
import com.laotang.core.ai.memory.ConversationMemoryService;
import com.laotang.core.ai.rag.KnowledgeContext;
import com.laotang.core.ai.rag.KnowledgeRetriever;
import com.laotang.core.ai.service.IAiChatService;
import com.laotang.core.ai.support.AiCircuitBreaker;
import com.laotang.core.ai.support.ConversationLock;
import com.laotang.framework.redis.ratelimit.RedisRateLimiter;
import com.laotang.parameter.ai.response.ChatReplyResponse;
import com.laotang.parameter.ai.response.ConversationResponse;
import com.laotang.parameter.ai.response.MessageResponse;
import com.laotang.parameter.ai.response.SourceResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * AI 客服会话服务实现。
 *
 * <p>对话主链路刻意不加长事务:模型调用可能持续数秒,
 * 把数据库连接握在手里会迅速耗尽连接池。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiChatServiceImpl implements IAiChatService {

    private static final String DEFAULT_TITLE = "新会话";
    private static final int TITLE_MAX_LENGTH = 20;
    private static final Duration RATE_WINDOW = Duration.ofMinutes(1);
    private static final String FALLBACK_REPLY =
            "抱歉,AI 助手暂时不可用,请稍后再试。您也可以联系人工客服(每天 9:00-21:00)。";

    private final AiConversationMapper conversationMapper;
    private final AiMessageMapper messageMapper;
    private final AiChatGateway chatGateway;
    private final AiToolFactory toolFactory;
    private final KnowledgeRetriever knowledgeRetriever;
    private final ConversationMemoryService conversationMemory;
    private final ConversationLock conversationLock;
    private final AiCircuitBreaker circuitBreaker;
    private final RedisRateLimiter rateLimiter;
    private final AiChatProperties properties;
    private final ObjectMapper objectMapper;

    @Override
    public ConversationResponse create(Long userId, RoleEnum role, AiScope scope, String title) {
        if (!scope.availableTo(role)) {
            throw new BusinessException(ErrorCode.AI_SCOPE_FORBIDDEN);
        }

        LocalDateTime now = LocalDateTime.now();
        AiConversation conversation = new AiConversation();
        conversation.setUserId(userId);
        conversation.setScope(scope.name());
        conversation.setTitle(normalizeTitle(title));
        conversation.setCreatedAt(now);
        conversation.setUpdatedAt(now);
        conversationMapper.insert(conversation);

        return AiConverter.toResponse(conversation);
    }

    @Override
    public PageResult<ConversationResponse> listConversations(
            Long userId, AiScope scope, long page, long size) {
        LambdaQueryWrapper<AiConversation> query = new LambdaQueryWrapper<AiConversation>()
                .eq(AiConversation::getUserId, userId)
                .eq(scope != null, AiConversation::getScope, scope == null ? null : scope.name())
                .orderByDesc(AiConversation::getUpdatedAt)
                .orderByDesc(AiConversation::getId);

        Page<AiConversation> result = conversationMapper.selectPage(new Page<>(page, size), query);
        List<ConversationResponse> list = result.getRecords().stream()
                .map(AiConverter::toResponse)
                .toList();

        return PageResult.of(list, result.getCurrent(), result.getSize(), result.getTotal());
    }

    @Override
    public PageResult<MessageResponse> listMessages(
            Long userId, Long conversationId, long page, long size) {
        requireOwned(userId, conversationId);

        Page<AiMessage> result = messageMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<AiMessage>()
                        .eq(AiMessage::getConversationId, conversationId)
                        .orderByAsc(AiMessage::getId));

        List<MessageResponse> list = result.getRecords().stream()
                .map(AiConverter::toResponse)
                .toList();

        return PageResult.of(list, result.getCurrent(), result.getSize(), result.getTotal());
    }

    @Override
    public ChatReplyResponse chat(Long userId, Long conversationId, String content) {
        if (!rateLimiter.tryAcquire(
                RedisKey.aiChatRate(userId),
                properties.getRateLimitPerMinute(),
                RATE_WINDOW)) {
            throw new BusinessException(ErrorCode.TOO_MANY_REQUESTS, "AI 请求过于频繁,请稍后再试");
        }

        AiConversation conversation = requireOwned(userId, conversationId);

        if (!conversationLock.tryLock(conversationId)) {
            throw new BusinessException(ErrorCode.AI_CONVERSATION_BUSY);
        }
        try {
            return doChat(userId, conversation, content);
        } finally {
            conversationLock.unlock(conversationId);
        }
    }

    private ChatReplyResponse doChat(
            Long userId, AiConversation conversation, String content) {

        Long conversationId = conversation.getId();
        AiScope scope = AiScope.from(conversation.getScope());

        // 先刷新摘要:旧消息进入摘要,最近窗口仍保留原文
        String summary = conversationMemory.refreshSummary(conversation);
        List<ChatTurn> history = loadHistory(conversationId);

        String question = content.strip();
        LocalDateTime now = LocalDateTime.now();

        AiMessage userMessage = buildMessage(conversationId, AiMessageRole.USER, question, now);
        messageMapper.insert(userMessage);
        touchConversation(conversation, question, now);

        // 熔断打开时直接降级,不再做检索和模型调用
        if (circuitBreaker.isOpen()) {
            return persistFallback(conversationId);
        }

        KnowledgeContext knowledge = knowledgeRetriever.retrieve(scope, question);

        AiToolRecorder recorder = new AiToolRecorder();
        List<Object> tools = toolFactory.create(scope, userId, recorder);

        ChatResult result;
        boolean degraded = false;
        try {
            result = chatGateway.chat(new AiChatCommand(
                    scope, history, question, tools, knowledge, summary));
        } catch (BusinessException ex) {
            if (ex.getErrorCode() != ErrorCode.AI_SERVICE_UNAVAILABLE) {
                throw ex;
            }
            log.warn("AI 调用失败,返回降级话术: {}", ex.getMessage());
            result = new ChatResult(FALLBACK_REPLY, 0, 0);
            degraded = true;
        }

        LocalDateTime repliedAt = LocalDateTime.now();
        AiMessage assistantMessage = buildMessage(
                conversationId, AiMessageRole.ASSISTANT, result.content(), repliedAt);
        assistantMessage.setTokensIn(result.tokensIn());
        assistantMessage.setTokensOut(result.tokensOut());
        if (!degraded && !recorder.isEmpty()) {
            assistantMessage.setToolCalls(serializeToolCalls(recorder));
        }
        messageMapper.insert(assistantMessage);
        markReplied(conversationId, repliedAt);

        List<SourceResponse> sources = degraded ? List.of()
                : knowledge.snippets().stream()
                .map(snippet -> new SourceResponse(
                        snippet.docId(),
                        snippet.title(),
                        snippet.section(),
                        Math.round(snippet.score() * 1000) / 1000.0))
                .toList();

        return new ChatReplyResponse(
                String.valueOf(conversationId),
                String.valueOf(assistantMessage.getId()),
                result.content(),
                result.tokensIn(),
                result.tokensOut(),
                DateTimeUtils.toIso(assistantMessage.getCreatedAt()),
                sources,
                degraded);
    }

    /**
     * 熔断打开时的固定降级回复,仍然落库,保证历史完整。
     */
    private ChatReplyResponse persistFallback(Long conversationId) {
        LocalDateTime now = LocalDateTime.now();
        AiMessage assistantMessage = buildMessage(
                conversationId, AiMessageRole.ASSISTANT, FALLBACK_REPLY, now);
        messageMapper.insert(assistantMessage);
        markReplied(conversationId, now);

        return new ChatReplyResponse(
                String.valueOf(conversationId),
                String.valueOf(assistantMessage.getId()),
                FALLBACK_REPLY,
                0,
                0,
                DateTimeUtils.toIso(now),
                List.of(),
                true);
    }

    @Override
    @Transactional
    public void delete(Long userId, Long conversationId) {
        requireOwned(userId, conversationId);
        messageMapper.delete(new LambdaQueryWrapper<AiMessage>()
                .eq(AiMessage::getConversationId, conversationId));
        conversationMapper.deleteById(conversationId);
    }

    private AiConversation requireOwned(Long userId, Long conversationId) {
        AiConversation conversation = conversationMapper.selectById(conversationId);
        if (conversation == null || !conversation.getUserId().equals(userId)) {
            // 不区分"不存在"和"不属于你",避免泄露他人会话是否存在
            throw new BusinessException(ErrorCode.AI_CONVERSATION_NOT_FOUND);
        }
        return conversation;
    }

    private List<ChatTurn> loadHistory(Long conversationId) {
        Page<AiMessage> page = new Page<>(1, properties.getHistoryWindow(), false);
        Page<AiMessage> result = messageMapper.selectPage(page,
                new LambdaQueryWrapper<AiMessage>()
                        .eq(AiMessage::getConversationId, conversationId)
                        .orderByDesc(AiMessage::getId));

        List<AiMessage> recent = new ArrayList<>(result.getRecords());
        Collections.reverse(recent);

        return recent.stream()
                .filter(message -> AiMessageRole.USER.name().equals(message.getRole())
                        || AiMessageRole.ASSISTANT.name().equals(message.getRole()))
                .map(message -> new ChatTurn(
                        AiMessageRole.valueOf(message.getRole()), message.getContent()))
                .toList();
    }

    private AiMessage buildMessage(
            Long conversationId, AiMessageRole role, String content, LocalDateTime createdAt) {
        AiMessage message = new AiMessage();
        message.setConversationId(conversationId);
        message.setRole(role.name());
        message.setContent(content);
        message.setTokensIn(0);
        message.setTokensOut(0);
        message.setCreatedAt(createdAt);
        return message;
    }

    private void touchConversation(AiConversation conversation, String question, LocalDateTime now) {
        LambdaUpdateWrapper<AiConversation> update = new LambdaUpdateWrapper<AiConversation>()
                .eq(AiConversation::getId, conversation.getId())
                .set(AiConversation::getLastMessageAt, now)
                .set(AiConversation::getUpdatedAt, now);

        if (conversation.getTitle() == null || conversation.getTitle().isBlank()
                || DEFAULT_TITLE.equals(conversation.getTitle())) {
            update.set(AiConversation::getTitle, deriveTitle(question));
        }
        conversationMapper.update(null, update);
    }

    private String serializeToolCalls(AiToolRecorder recorder) {
        try {
            return objectMapper.writeValueAsString(recorder.calls());
        } catch (JsonProcessingException ex) {
            log.warn("序列化工具调用记录失败", ex);
            return null;
        }
    }

    /**
     * 助手回复后把会话的"最后消息时间"推进到回复时间,列表排序才准确。
     */
    private void markReplied(Long conversationId, LocalDateTime repliedAt) {
        conversationMapper.update(null, new LambdaUpdateWrapper<AiConversation>()
                .eq(AiConversation::getId, conversationId)
                .set(AiConversation::getLastMessageAt, repliedAt)
                .set(AiConversation::getUpdatedAt, repliedAt));
    }

    private String normalizeTitle(String title) {
        if (title == null || title.isBlank()) {
            return DEFAULT_TITLE;
        }
        return truncate(title.strip());
    }

    private String deriveTitle(String question) {
        return truncate(question.replaceAll("\\s+", " ").strip());
    }

    private String truncate(String text) {
        if (text.length() <= TITLE_MAX_LENGTH) {
            return text;
        }
        return text.substring(0, TITLE_MAX_LENGTH) + "…";
    }
}
