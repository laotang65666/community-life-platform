package com.laotang.core.ai.memory;

import com.laotang.core.ai.gateway.ChatTurn;

import java.util.List;

/**
 * 会话摘要生成器。
 */
public interface SummaryGenerator {

    /**
     * 把已有摘要与新增对话合并成新的摘要。
     */
    String summarize(String existingSummary, List<ChatTurn> turns);
}
