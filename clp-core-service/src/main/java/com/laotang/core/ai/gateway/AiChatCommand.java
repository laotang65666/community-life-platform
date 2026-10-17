package com.laotang.core.ai.gateway;

import com.laotang.core.ai.enums.AiScope;
import com.laotang.core.ai.rag.KnowledgeContext;

import java.util.List;

/**
 * 一次模型调用的输入。
 *
 * @param scope       助手范围,决定系统提示词
 * @param history     历史对话(不含当前问题)
 * @param userContent 当前问题
 * @param tools       本次可调用的工具实例
 * @param knowledge   RAG 命中的参考资料,可能为空
 * @param summary     更早对话的摘要,可能为空
 */
public record AiChatCommand(
        AiScope scope,
        List<ChatTurn> history,
        String userContent,
        List<Object> tools,
        KnowledgeContext knowledge,
        String summary
) {
}
