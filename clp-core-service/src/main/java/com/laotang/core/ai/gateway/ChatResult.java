package com.laotang.core.ai.gateway;

/**
 * 模型返回。
 *
 * @param content   助手回复正文
 * @param tokensIn  输入 token 数
 * @param tokensOut 输出 token 数
 */
public record ChatResult(String content, int tokensIn, int tokensOut) {
}
