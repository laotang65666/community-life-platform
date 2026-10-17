package com.laotang.core.ai.rag;

import com.laotang.core.ai.enums.AiScope;
import com.laotang.parameter.ai.request.RagEvalCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RagEvaluationServiceTest {

    @Mock
    private KnowledgeRetriever knowledgeRetriever;
    @InjectMocks
    private RagEvaluationService evaluationService;

    @Test
    @DisplayName("按首次命中名次计算 HitRate 与 MRR")
    void evaluate_metrics() {
        when(knowledgeRetriever.retrieve(AiScope.USER, "问题一"))
                .thenReturn(context("seed:doc-a"));
        when(knowledgeRetriever.retrieve(AiScope.USER, "问题二"))
                .thenReturn(context("seed:doc-x"));
        when(knowledgeRetriever.retrieve(AiScope.USER, "问题三"))
                .thenReturn(context("seed:doc-x", "seed:doc-b"));

        var response = evaluationService.evaluate(AiScope.USER, List.of(
                new RagEvalCase("问题一", List.of("seed:doc-a")),
                new RagEvalCase("问题二", List.of("seed:doc-b")),
                new RagEvalCase("问题三", List.of("seed:doc-b"))));

        assertThat(response.total()).isEqualTo(3);
        assertThat(response.hits()).isEqualTo(2);
        assertThat(response.hitRate()).isEqualTo(0.667);
        assertThat(response.mrr()).isEqualTo(0.5);
        assertThat(response.details().get(1).hit()).isFalse();
        assertThat(response.details().get(2).rank()).isEqualTo(2);
    }

    private KnowledgeContext context(String... docIds) {
        return new KnowledgeContext(java.util.Arrays.stream(docIds)
                .map(docId -> new KnowledgeSnippet(docId, "标题", "章节", "内容", 0.9))
                .toList());
    }
}
