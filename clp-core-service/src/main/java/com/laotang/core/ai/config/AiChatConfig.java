package com.laotang.core.ai.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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
}
