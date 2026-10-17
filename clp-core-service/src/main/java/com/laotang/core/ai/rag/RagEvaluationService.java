package com.laotang.core.ai.rag;

import com.laotang.core.ai.enums.AiScope;
import com.laotang.parameter.ai.request.RagEvalCase;
import com.laotang.parameter.ai.response.RagEvalDetail;
import com.laotang.parameter.ai.response.RagEvalResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * RAG 检索效果评估。
 *
 * <p>指标只看"期望文档是否出现在最终证据里":</p>
 * <ul>
 *   <li>HitRate:命中用例数 / 总用例数;</li>
 *   <li>MRR:首次命中名次的倒数均值,越接近 1 说明正确证据排得越靠前。</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RagEvaluationService {

    private final KnowledgeRetriever knowledgeRetriever;

    public RagEvalResponse evaluate(AiScope scope, List<RagEvalCase> cases) {
        int hits = 0;
        double reciprocalRankSum = 0.0;
        List<RagEvalDetail> details = new ArrayList<>(cases.size());

        for (RagEvalCase evalCase : cases) {
            KnowledgeContext context = knowledgeRetriever.retrieve(scope, evalCase.question());
            int rank = 0;
            for (int i = 0; i < context.snippets().size(); i++) {
                if (evalCase.expectedDocIds().contains(context.snippets().get(i).docId())) {
                    rank = i + 1;
                    break;
                }
            }
            boolean hit = rank > 0;
            if (hit) {
                hits++;
                reciprocalRankSum += 1.0 / rank;
            }
            details.add(new RagEvalDetail(
                    evalCase.question(), hit, rank, context.snippets().size()));
        }

        int total = cases.size();
        double hitRate = total == 0 ? 0.0 : (double) hits / total;
        double mrr = total == 0 ? 0.0 : reciprocalRankSum / total;

        RagEvalResponse response = new RagEvalResponse(
                total, hits, round(hitRate), round(mrr), details);
        log.info("RAG 评估完成: scope={}, total={}, hits={}, hitRate={}, mrr={}",
                scope, total, hits, response.hitRate(), response.mrr());
        return response;
    }

    private double round(double value) {
        return Math.round(value * 1000) / 1000.0;
    }
}
