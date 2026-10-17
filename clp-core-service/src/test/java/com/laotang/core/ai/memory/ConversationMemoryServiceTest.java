package com.laotang.core.ai.memory;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.laotang.core.ai.config.AiChatProperties;
import com.laotang.core.ai.entity.AiConversation;
import com.laotang.core.ai.entity.AiMessage;
import com.laotang.core.ai.mapper.AiConversationMapper;
import com.laotang.core.ai.mapper.AiMessageMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConversationMemoryServiceTest {

    @Mock
    private AiMessageMapper messageMapper;
    @Mock
    private AiConversationMapper conversationMapper;
    @Mock
    private SummaryGenerator summaryGenerator;

    private AiChatProperties properties;
    private ConversationMemoryService memoryService;

    @BeforeAll
    static void initTableInfo() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, AiMessage.class);
        TableInfoHelper.initTableInfo(assistant, AiConversation.class);
    }

    @BeforeEach
    void setUp() {
        properties = new AiChatProperties();
        properties.setHistoryWindow(5);
        properties.setSummaryTriggerMessages(10);
        properties.setSummaryMaxChars(200);
        memoryService = new ConversationMemoryService(
                messageMapper, conversationMapper, summaryGenerator, properties);
    }

    private AiConversation conversation() {
        AiConversation conversation = new AiConversation();
        conversation.setId(100L);
        conversation.setSummary("旧摘要");
        conversation.setSummaryMessageId(50L);
        return conversation;
    }

    @Test
    @DisplayName("未摘要消息未达阈值时不生成摘要")
    void belowThreshold_keepsOldSummary() {
        AiConversation conversation = conversation();
        when(messageMapper.selectCount(any())).thenReturn(8L);

        String summary = memoryService.refreshSummary(conversation);

        assertThat(summary).isEqualTo("旧摘要");
        verify(summaryGenerator, never()).summarize(any(), any());
    }

    @Test
    @DisplayName("超过阈值时摘要最旧的消息并推进 summary_message_id")
    void aboveThreshold_summarizesOldest() {
        AiConversation conversation = conversation();
        when(messageMapper.selectCount(any())).thenReturn(15L);
        List<AiMessage> messages = new ArrayList<>();
        for (long id = 51; id <= 60; id++) {
            AiMessage message = new AiMessage();
            message.setId(id);
            message.setRole(id % 2 == 0 ? "ASSISTANT" : "USER");
            message.setContent("消息" + id);
            messages.add(message);
        }
        when(messageMapper.selectList(any())).thenReturn(messages);
        when(summaryGenerator.summarize(any(), any())).thenReturn("新摘要");

        String summary = memoryService.refreshSummary(conversation);

        assertThat(summary).isEqualTo("新摘要");
        assertThat(conversation.getSummary()).isEqualTo("新摘要");
        assertThat(conversation.getSummaryMessageId()).isEqualTo(60L);
        verify(conversationMapper).update(isNull(), any());
    }

    @Test
    @DisplayName("摘要失败时保留旧摘要,不推进进度")
    void summaryFailure_keepsOldSummary() {
        AiConversation conversation = conversation();
        when(messageMapper.selectCount(any())).thenReturn(15L);
        AiMessage message = new AiMessage();
        message.setId(51L);
        message.setRole("USER");
        message.setContent("消息");
        when(messageMapper.selectList(any())).thenReturn(List.of(message));
        when(summaryGenerator.summarize(any(), any())).thenThrow(new IllegalStateException("模型不可用"));

        String summary = memoryService.refreshSummary(conversation);

        assertThat(summary).isEqualTo("旧摘要");
        assertThat(conversation.getSummaryMessageId()).isEqualTo(50L);
        verify(conversationMapper, never()).update(any(), any());
    }

    @Test
    @DisplayName("关闭摘要功能时直接返回旧摘要")
    void disabled_returnsOldSummary() {
        properties.setSummaryEnabled(false);

        assertThat(memoryService.refreshSummary(conversation())).isEqualTo("旧摘要");
        verify(messageMapper, never()).selectCount(any());
    }
}
