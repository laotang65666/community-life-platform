package com.laotang.core.ai.gateway;

import com.laotang.common.exception.BusinessException;
import com.laotang.common.exception.ErrorCode;
import com.laotang.core.ai.enums.AiMessageRole;
import com.laotang.core.ai.prompt.AiPromptProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 基于 Spring AI 的模型调用实现。
 *
 * <p>只负责拼装消息、注册工具、发起调用、把异常翻译成业务错误码;
 * 会话归属、限流、落库、工具记录都在业务层完成。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SpringAiChatGateway implements AiChatGateway {

    private final ChatClient chatClient;
    private final AiPromptProvider promptProvider;

    @Override
    public ChatResult chat(AiChatCommand command) {
        List<Message> messages = new ArrayList<>(command.history().size());
        for (ChatTurn turn : command.history()) {
            append(messages, turn);
        }

        try {
            ChatClient.ChatClientRequestSpec spec = chatClient.prompt()
                    .system(systemPrompt(command))
                    .messages(messages)
                    .user(command.userContent());

            if (command.tools() != null && !command.tools().isEmpty()) {
                // Spring AI 会在模型返回 tool_calls 时自动执行工具并把结果回传模型,
                // 直到模型给出最终文本回答。
                spec = spec.tools(command.tools().toArray());
            }

            ChatResponse response = spec.call().chatResponse();

            String content = response == null || response.getResult() == null
                    ? ""
                    : response.getResult().getOutput().getText();

            if (content == null || content.isBlank()) {
                throw new BusinessException(ErrorCode.AI_SERVICE_UNAVAILABLE, "AI 未返回有效内容");
            }

            Usage usage = response.getMetadata() == null ? null : response.getMetadata().getUsage();
            int tokensIn = usage == null || usage.getPromptTokens() == null ? 0 : usage.getPromptTokens();
            int tokensOut = usage == null || usage.getCompletionTokens() == null ? 0 : usage.getCompletionTokens();

            return new ChatResult(content, tokensIn, tokensOut);
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            // 模型供应商的异常类型随版本变化,这里作为边界统一收敛成业务错误,
            // 避免把 SDK 细节泄露到上层。
            log.error("调用大模型失败, scope={}, historySize={}",
                    command.scope(), command.history().size(), ex);
            throw new BusinessException(ErrorCode.AI_SERVICE_UNAVAILABLE);
        }
    }

    private String systemPrompt(AiChatCommand command) {
        String base = promptProvider.systemPrompt(command.scope());
        if (command.knowledge() != null && !command.knowledge().isEmpty()) {
            return base + "\n\n" + command.knowledge().toPromptBlock();
        }
        return base + """


                ## 参考资料
                本轮未检索到平台资料。
                - 只回答工具查询到的事实。
                - 涉及平台规则、流程、时间、金额时不要推测,直接建议用户联系人工客服。
                - 不要用"通常""可能""一般"来编造平台规则。
                """;
    }

    private void append(List<Message> messages, ChatTurn turn) {
        if (turn.content() == null || turn.content().isBlank()) {
            return;
        }
        if (turn.role() == AiMessageRole.ASSISTANT) {
            messages.add(new AssistantMessage(turn.content()));
        } else if (turn.role() == AiMessageRole.USER) {
            messages.add(new UserMessage(turn.content()));
        }
        // TOOL 结果由 Spring AI 在单次工具调用内处理,不进入跨轮历史
    }
}
