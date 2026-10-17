package com.laotang.core.ai.gateway;

import com.laotang.core.ai.enums.AiMessageRole;

/**
 * 送给模型的一段历史对话。
 */
public record ChatTurn(AiMessageRole role, String content) {
}
