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

    /**
     * 单次模型调用总超时。
     */
    private long timeoutMs = 15000;

    /**
     * 连续失败多少次后打开熔断器。
     */
    private int circuitFailureThreshold = 5;

    /**
     * 熔断器打开时长。
     */
    private long circuitOpenMs = 30000;

    /**
     * 是否启用会话摘要记忆。
     */
    private boolean summaryEnabled = true;

    /**
     * 未摘要消息超过该条数时触发摘要。
     */
    private int summaryTriggerMessages = 30;

    /**
     * 摘要最大字数。
     */
    private int summaryMaxChars = 300;
}
