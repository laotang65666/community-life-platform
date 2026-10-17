package com.laotang.core.ai.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * AI 对话相关 Bean。
 */
@Configuration
public class AiChatConfig {

    /**
     * Spring AI 自动配置提供的是 Builder,这里固化成单例 ChatClient。
     */
    @Bean
    public ChatClient chatClient(ChatClient.Builder builder) {
        return builder.build();
    }

    /**
     * 模型调用专用执行器。
     *
     * <p>用虚拟线程承载阻塞式 HTTP 调用,配合 {@code Future.get(timeout)}
     * 给整轮模型调用一个确定的上界,避免请求线程被无限期挂住。</p>
     */
    @Bean(destroyMethod = "close")
    public ExecutorService aiChatExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }
}
