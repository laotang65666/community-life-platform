package com.laotang.core.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * AI 对话的业务参数。
 */
@Data
@ConfigurationProperties(prefix = "clp.ai.chat")
public class AiChatProperties {

    /**
     * 每次请求带上的历史消息条数(只取最近 N 条)。
     */
    private int historyWindow = 20;

    /**
     * 单用户每分钟最多请求次数。
     */
    private int rateLimitPerMinute = 20;
}
