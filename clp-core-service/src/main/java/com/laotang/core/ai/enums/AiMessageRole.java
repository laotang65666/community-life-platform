package com.laotang.core.ai.enums;

/**
 * 消息角色,与 {@code tb_ai_message.role} 保持一致。
 */
public enum AiMessageRole {

    USER,

    ASSISTANT,

    /**
     * 工具调用结果,当前阶段仅预留。
     */
    TOOL
}
