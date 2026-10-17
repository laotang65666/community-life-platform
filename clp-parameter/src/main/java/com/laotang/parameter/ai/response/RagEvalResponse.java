package com.laotang.parameter.ai.response;

import java.util.List;

/**
 * RAG 检索评估结果。
 *
 * @param hitRate 命中率 = 命中用例数 / 总用例数
 * @param mrr     平均倒数排名 = Σ(1/首次命中名次) / 总用例数
 */
public record RagEvalResponse(
        int total,
        int hits,
        double hitRate,
        double mrr,
        List<RagEvalDetail> details
) {
}
