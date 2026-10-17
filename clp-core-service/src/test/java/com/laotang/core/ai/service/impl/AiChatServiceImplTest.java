package com.laotang.core.ai.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.laotang.common.enums.RoleEnum;
import com.laotang.common.exception.BusinessException;
import com.laotang.common.exception.ErrorCode;
import com.laotang.core.ai.config.AiChatProperties;
import com.laotang.core.ai.entity.AiConversation;
import com.laotang.core.ai.entity.AiMessage;
import com.laotang.core.ai.enums.AiScope;
import com.laotang.core.ai.gateway.AiChatCommand;
import com.laotang.core.ai.gateway.AiChatGateway;
import com.laotang.core.ai.gateway.ChatResult;
import com.laotang.core.ai.mapper.AiConversationMapper;
import com.laotang.core.ai.mapper.AiMessageMapper;
import com.laotang.core.ai.tool.AiToolFactory;
import com.laotang.core.ai.tool.AiToolRecorder;
import com.laotang.framework.redis.ratelimit.RedisRateLimiter;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiChatServiceImplTest {

    @Mock
    private AiConversationMapper conversationMapper;
    @Mock
    private AiMessageMapper messageMapper;
    @Mock
    private AiChatGateway chatGateway;
    @Mock
    private AiToolFactory toolFactory;
    @Mock
    private RedisRateLimiter rateLimiter;

    private AiChatServiceImpl service;

    @BeforeAll
    static void initTableInfo() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, AiConversation.class);
        TableInfoHelper.initTableInfo(assistant, AiMessage.class);
    }

    @BeforeEach
    void setUp() {
        AiChatProperties properties = new AiChatProperties();
        properties.setHistoryWindow(5);
        properties.setRateLimitPerMinute(2);
        service = new AiChatServiceImpl(
                conversationMapper,
                messageMapper,
                chatGateway,
                toolFactory,
                rateLimiter,
                properties,
                new ObjectMapper());
    }

    private AiConversation conversation() {
        AiConversation conversation = new AiConversation();
        conversation.setId(100L);
        conversation.setUserId(1L);
        conversation.setScope("USER");
        conversation.setTitle("新会话");
        return conversation;
    }

    @Test
    @DisplayName("普通用户不能创建商户端会话")
    void create_merchantScopeWithUserRole_forbidden() {
        BusinessException ex = catchThrowableOfType(
                () -> service.create(1L, RoleEnum.USER, AiScope.MERCHANT, null),
                BusinessException.class);

        assertThat(ex).isNotNull();
        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.AI_SCOPE_FORBIDDEN);
        verify(conversationMapper, never()).insert(any(AiConversation.class));
    }

    @Test
    @DisplayName("创建会话时标题为空使用默认标题")
    void create_defaultTitle() {
        when(conversationMapper.insert(any(AiConversation.class))).thenAnswer(inv -> {
            AiConversation saved = inv.getArgument(0);
            saved.setId(100L);
            return 1;
        });

        var response = service.create(1L, RoleEnum.USER, AiScope.USER, "  ");

        assertThat(response.id()).isEqualTo("100");
        assertThat(response.scope()).isEqualTo("USER");
        assertThat(response.title()).isEqualTo("新会话");
    }

    @Test
    @DisplayName("正常问答:落库用户消息与助手消息,并返回 token 统计")
    void chat_happyPath() {
        when(rateLimiter.tryAcquire(anyString(), eq(2), any())).thenReturn(true);
        when(conversationMapper.selectById(100L)).thenReturn(conversation());
        when(messageMapper.selectPage(any(), any())).thenReturn(new Page<>(1, 5, false));
        when(messageMapper.insert(any(AiMessage.class))).thenAnswer(inv -> {
            AiMessage message = inv.getArgument(0);
            message.setId("USER".equals(message.getRole()) ? 200L : 201L);
            return 1;
        });
        when(toolFactory.create(eq(AiScope.USER), eq(1L), any())).thenReturn(List.of());
        when(chatGateway.chat(any(AiChatCommand.class)))
                .thenReturn(new ChatResult("进入订单详情页,点击取消订单。", 30, 12));

        var response = service.chat(1L, 100L, " 怎么取消订单? ");

        assertThat(response.conversationId()).isEqualTo("100");
        assertThat(response.messageId()).isEqualTo("201");
        assertThat(response.content()).isEqualTo("进入订单详情页,点击取消订单。");
        assertThat(response.tokensIn()).isEqualTo(30);
        assertThat(response.tokensOut()).isEqualTo(12);

        ArgumentCaptor<AiMessage> captor = ArgumentCaptor.forClass(AiMessage.class);
        verify(messageMapper, times(2)).insert(captor.capture());
        assertThat(captor.getAllValues())
                .extracting(AiMessage::getRole)
                .containsExactly("USER", "ASSISTANT");
        verify(conversationMapper, times(2)).update(isNull(), any());
    }

    @Test
    @DisplayName("工具调用记录写入 assistant 消息的 tool_calls")
    void chat_persistsToolCalls() {
        when(rateLimiter.tryAcquire(anyString(), eq(2), any())).thenReturn(true);
        when(conversationMapper.selectById(100L)).thenReturn(conversation());
        when(messageMapper.selectPage(any(), any())).thenReturn(new Page<>(1, 5, false));
        when(messageMapper.insert(any(AiMessage.class))).thenAnswer(inv -> {
            AiMessage message = inv.getArgument(0);
            message.setId("USER".equals(message.getRole()) ? 200L : 201L);
            return 1;
        });
        when(toolFactory.create(eq(AiScope.USER), eq(1L), any(AiToolRecorder.class)))
                .thenAnswer(inv -> {
                    AiToolRecorder recorder = inv.getArgument(2);
                    recorder.record("queryMyOrders", "limit=5", true, "ok", 12);
                    return List.of();
                });
        when(chatGateway.chat(any(AiChatCommand.class)))
                .thenReturn(new ChatResult("你有 3 笔订单。", 100, 20));

        service.chat(1L, 100L, "我最近有哪些订单?");

        ArgumentCaptor<AiMessage> captor = ArgumentCaptor.forClass(AiMessage.class);
        verify(messageMapper, times(2)).insert(captor.capture());
        AiMessage assistant = captor.getAllValues().get(1);
        assertThat(assistant.getToolCalls())
                .contains("queryMyOrders")
                .contains("\"success\":true");
    }

    @Test
    @DisplayName("超过每分钟限流后不再调用模型")
    void chat_rateLimited() {
        when(rateLimiter.tryAcquire(anyString(), eq(2), any())).thenReturn(false);

        BusinessException ex = catchThrowableOfType(
                () -> service.chat(1L, 100L, "你好"), BusinessException.class);

        assertThat(ex).isNotNull();
        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.TOO_MANY_REQUESTS);
        verify(conversationMapper, never()).selectById(any());
        verify(chatGateway, never()).chat(any(AiChatCommand.class));
    }

    @Test
    @DisplayName("会话不属于当前用户时按不存在处理")
    void chat_notOwner_notFound() {
        when(rateLimiter.tryAcquire(anyString(), eq(2), any())).thenReturn(true);
        AiConversation other = conversation();
        other.setUserId(2L);
        when(conversationMapper.selectById(100L)).thenReturn(other);

        BusinessException ex = catchThrowableOfType(
                () -> service.chat(1L, 100L, "你好"), BusinessException.class);

        assertThat(ex).isNotNull();
        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.AI_CONVERSATION_NOT_FOUND);
        verify(chatGateway, never()).chat(any(AiChatCommand.class));
    }

    @Test
    @DisplayName("会话列表按 scope 过滤")
    void listConversations_withScope() {
        Page<AiConversation> page = new Page<>(1, 20);
        page.setRecords(List.of(conversation()));
        page.setTotal(1);
        when(conversationMapper.selectPage(any(), any())).thenReturn(page);

        var result = service.listConversations(1L, AiScope.USER, 1, 20);

        assertThat(result.total()).isEqualTo(1);
        assertThat(result.list()).hasSize(1);
        assertThat(result.list().getFirst().scope()).isEqualTo("USER");
    }

    @Test
    @DisplayName("消息列表按时间正序返回")
    void listMessages() {
        when(conversationMapper.selectById(100L)).thenReturn(conversation());

        AiMessage message = new AiMessage();
        message.setId(200L);
        message.setConversationId(100L);
        message.setRole("USER");
        message.setContent("你好");
        message.setTokensIn(0);
        message.setTokensOut(0);

        Page<AiMessage> page = new Page<>(1, 50);
        page.setRecords(List.of(message));
        page.setTotal(1);
        when(messageMapper.selectPage(any(), any())).thenReturn(page);

        var result = service.listMessages(1L, 100L, 1, 50);

        assertThat(result.total()).isEqualTo(1);
        assertThat(result.list().getFirst().content()).isEqualTo("你好");
    }

    @Test
    @DisplayName("删除会话时先删消息再删会话")
    void delete_removesMessagesAndConversation() {
        when(conversationMapper.selectById(100L)).thenReturn(conversation());

        service.delete(1L, 100L);

        verify(messageMapper).delete(any());
        verify(conversationMapper).deleteById(100L);
    }
}
