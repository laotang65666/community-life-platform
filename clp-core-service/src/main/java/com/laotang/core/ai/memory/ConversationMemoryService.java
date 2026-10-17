package com.laotang.core.ai.memory;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.laotang.core.ai.config.AiChatProperties;
import com.laotang.core.ai.entity.AiConversation;
import com.laotang.core.ai.entity.AiMessage;
import com.laotang.core.ai.enums.AiMessageRole;
import com.laotang.core.ai.gateway.ChatTurn;
import com.laotang.core.ai.mapper.AiConversationMapper;
import com.laotang.core.ai.mapper.AiMessageMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 会话记忆:把超出最近窗口的旧消息压缩成摘要。
 *
 * <p>策略:始终保留最近 {@code history-window} 条原文;当未摘要消息超过
 * {@code summary-trigger-messages} 时,把最旧的一部分合并进摘要,
 * 并记录 {@code summary_message_id},避免重复摘要。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConversationMemoryService {

    private final AiMessageMapper messageMapper;
    private final AiConversationMapper conversationMapper;
    private final SummaryGenerator summaryGenerator;
    private final AiChatProperties properties;

    /**
     * 按需刷新摘要,返回当前摘要(可能为空)。摘要失败不影响主链路。
     */
    public String refreshSummary(AiConversation conversation) {
        if (!properties.isSummaryEnabled()) {
            return conversation.getSummary();
        }

        long summarizedUpTo = conversation.getSummaryMessageId() == null
                ? 0L : conversation.getSummaryMessageId();
        Long unsummarized = messageMapper.selectCount(
                new LambdaQueryWrapper<AiMessage>()
                        .eq(AiMessage::getConversationId, conversation.getId())
                        .gt(AiMessage::getId, summarizedUpTo));
        if (unsummarized == null
                || unsummarized <= properties.getSummaryTriggerMessages()) {
            return conversation.getSummary();
        }

        int keepRecent = properties.getHistoryWindow();
        int toSummarize = (int) Math.max(1, unsummarized - keepRecent);

        List<AiMessage> messages = messageMapper.selectList(
                new LambdaQueryWrapper<AiMessage>()
                        .eq(AiMessage::getConversationId, conversation.getId())
                        .gt(AiMessage::getId, summarizedUpTo)
                        .orderByAsc(AiMessage::getId)
                        .last("LIMIT " + toSummarize));
        if (messages.isEmpty()) {
            return conversation.getSummary();
        }

        List<ChatTurn> turns = messages.stream()
                .filter(message -> AiMessageRole.USER.name().equals(message.getRole())
                        || AiMessageRole.ASSISTANT.name().equals(message.getRole()))
                .map(message -> new ChatTurn(
                        AiMessageRole.valueOf(message.getRole()), message.getContent()))
                .toList();
        if (turns.isEmpty()) {
            return conversation.getSummary();
        }

        String summary;
        try {
            summary = summaryGenerator.summarize(conversation.getSummary(), turns);
        } catch (Exception ex) {
            // 摘要失败不能影响对话,也不能推进 summary_message_id
            log.warn("会话摘要生成失败,保留旧摘要: {}", ex.getMessage());
            return conversation.getSummary();
        }

        Long lastSummarizedId = messages.getLast().getId();
        conversationMapper.update(null, new LambdaUpdateWrapper<AiConversation>()
                .eq(AiConversation::getId, conversation.getId())
                .set(AiConversation::getSummary, summary)
                .set(AiConversation::getSummaryMessageId, lastSummarizedId)
                .set(AiConversation::getUpdatedAt, LocalDateTime.now()));

        conversation.setSummary(summary);
        conversation.setSummaryMessageId(lastSummarizedId);
        log.info("会话摘要已更新: conversationId={}, summarizedUpTo={}",
                conversation.getId(), lastSummarizedId);
        return summary;
    }
}
