package com.laotang.parameter.ai.response;

/**
 * 单条评估结果。
 *
 * @param hit         是否命中期望文档
 * @param rank        首次命中的名次,未命中为 0
 * @param sourceCount 本次检索返回的证据数
 */
public record RagEvalDetail(
        String question,
        boolean hit,
        int rank,
        int sourceCount
) {
}
