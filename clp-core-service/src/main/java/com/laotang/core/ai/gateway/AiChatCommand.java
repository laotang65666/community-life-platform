package com.laotang.core.ai.gateway;

import com.laotang.core.ai.enums.AiScope;

import java.util.List;

/**
 * 一次模型调用的输入。
 *
 * @param scope       助手范围,决定系统提示词
 * @param history     历史对话(不含当前问题)
 * @param userContent 当前问题
 * @param tools       本次可调用的工具实例
 */
public record AiChatCommand(
        AiScope scope,
        List<ChatTurn> history,
        String userContent,
        List<Object> tools
) {
}
