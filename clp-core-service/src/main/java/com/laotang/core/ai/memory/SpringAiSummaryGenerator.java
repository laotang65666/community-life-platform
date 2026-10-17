package com.laotang.core.ai.memory;

import com.laotang.core.ai.config.AiChatProperties;
import com.laotang.core.ai.enums.AiMessageRole;
import com.laotang.core.ai.gateway.ChatTurn;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * 用同一个大模型生成会话摘要。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SpringAiSummaryGenerator implements SummaryGenerator {

    private static final String SYSTEM_PROMPT =
            "你是客服对话摘要器。只输出摘要正文,不回答问题,不编造对话中没有的信息。";

    private final ChatClient chatClient;
    private final AiChatProperties properties;
    private final ExecutorService aiChatExecutor;

    @Override
    public String summarize(String existingSummary, List<ChatTurn> turns) {
        String transcript = turns.stream()
                .map(turn -> (turn.role() == AiMessageRole.USER ? "用户: " : "助手: ")
                        + turn.content())
                .reduce((a, b) -> a + "\n" + b)
                .orElse("");

        String prompt = """
                把下面的客服对话压缩成不超过 %d 字的中文要点。
                保留:用户身份与诉求、已确认的事实、已给出的处理方案、未解决的问题。
                已有摘要:%s
                新增对话:
                %s
                """.formatted(
                properties.getSummaryMaxChars(),
                existingSummary == null || existingSummary.isBlank() ? "无" : existingSummary,
                transcript);

        Future<String> future = aiChatExecutor.submit(() -> chatClient.prompt()
                .system(SYSTEM_PROMPT)
                .user(prompt)
                .call()
                .content());

        String summary;
        try {
            summary = future.get(properties.getTimeoutMs(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException ex) {
            future.cancel(true);
            throw new IllegalStateException("摘要生成超时", ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            future.cancel(true);
            throw new IllegalStateException("摘要生成被中断", ex);
        } catch (ExecutionException ex) {
            throw new IllegalStateException("摘要生成失败", ex.getCause());
        }

        if (summary == null || summary.isBlank()) {
            throw new IllegalStateException("摘要结果为空");
        }
        return truncate(summary.strip(), properties.getSummaryMaxChars() * 2);
    }

    private String truncate(String text, int maxChars) {
        return text.length() <= maxChars ? text : text.substring(0, maxChars);
    }
}
