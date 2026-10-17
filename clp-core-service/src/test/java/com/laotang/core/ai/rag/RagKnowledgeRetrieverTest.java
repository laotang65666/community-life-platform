package com.laotang.core.ai.rag;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.laotang.common.constant.RedisKey;
import com.laotang.common.exception.BusinessException;
import com.laotang.common.exception.ErrorCode;
import com.laotang.core.ai.enums.AiScope;
import com.laotang.core.ai.rag.config.RagProperties;
import com.laotang.core.ai.rag.entity.KnowledgeChunk;
import com.laotang.core.ai.rag.mapper.KnowledgeChunkMapper;
import com.laotang.framework.ai.zhipu.EmbeddingClient;
import com.laotang.framework.ai.zhipu.RerankClient;
import com.laotang.framework.ai.zhipu.RerankResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RagKnowledgeRetrieverTest {

    @Mock
    private KnowledgeChunkMapper chunkMapper;
    @Mock
    private EmbeddingClient embeddingClient;
    @Mock
    private RerankClient rerankClient;
    @Mock
    private StringRedisTemplate redis;
    @Mock
    private ValueOperations<String, String> valueOps;

    private RagProperties properties;
    private ObjectMapper objectMapper;
    private RagKnowledgeRetriever retriever;

    @BeforeEach
    void setUp() {
        properties = new RagProperties();
        objectMapper = new ObjectMapper();
        KnowledgeVectorCache vectorCache = new KnowledgeVectorCache(objectMapper, properties);
        retriever = new RagKnowledgeRetriever(
                chunkMapper, embeddingClient, rerankClient, redis,
                objectMapper, properties, vectorCache);
        lenient().when(redis.opsForValue()).thenReturn(valueOps);
    }

    @Test
    @DisplayName("向量 + 关键词 → RRF 融合 → rerank → 证据阈值过滤")
    void retrieve_hybridAndRerank() {
        String query = "订单取消";
        when(embeddingClient.embed(query)).thenReturn(new float[]{1, 0});

        KnowledgeChunk a = chunk(1L, "A", "取消订单", "[1.0,0.0]");
        KnowledgeChunk b = chunk(2L, "B", "退款规则", "[0.0,1.0]");
        KnowledgeChunk c = chunk(3L, "C", "秒杀规则", "[0.9,0.1]");

        when(chunkMapper.selectVectorCandidates("USER", 200)).thenReturn(List.of(a, b));
        when(chunkMapper.selectByKeyword("USER", query, 20))
                .thenReturn(List.of(withScore(b, 5.0), withScore(c, 3.0)));
        when(rerankClient.rerank(eq(query), anyList(), eq(4)))
                .thenReturn(List.of(
                        new RerankResult(1, 0.92),
                        new RerankResult(0, 0.88),
                        new RerankResult(2, 0.20)));

        KnowledgeContext context = retriever.retrieve(AiScope.USER, query);

        assertThat(context.snippets())
                .extracting(KnowledgeSnippet::title)
                .containsExactly("A", "B");
        assertThat(context.snippets().getFirst().score()).isEqualTo(0.92);
    }

    @Test
    @DisplayName("rerank 不可用时降级为 RRF 排序")
    void retrieve_rerankUnavailable_fallsBackToRrf() {
        String query = "订单取消";
        when(embeddingClient.embed(query)).thenReturn(new float[]{1, 0});

        KnowledgeChunk a = chunk(1L, "A", "取消订单", "[1.0,0.0]");
        KnowledgeChunk b = chunk(2L, "B", "退款规则", "[0.0,1.0]");
        when(chunkMapper.selectVectorCandidates("USER", 200)).thenReturn(List.of(a, b));
        when(chunkMapper.selectByKeyword("USER", query, 20)).thenReturn(List.of());
        when(rerankClient.rerank(eq(query), anyList(), eq(4)))
                .thenThrow(new BusinessException(ErrorCode.AI_SERVICE_UNAVAILABLE));

        KnowledgeContext context = retriever.retrieve(AiScope.USER, query);

        assertThat(context.snippets())
                .extracting(KnowledgeSnippet::title)
                .containsExactly("A", "B");
    }

    @Test
    @DisplayName("向量服务不可用时只走关键词召回")
    void retrieve_embeddingUnavailable_usesKeyword() {
        String query = "订单取消";
        when(embeddingClient.embed(query))
                .thenThrow(new BusinessException(ErrorCode.AI_SERVICE_UNAVAILABLE));

        KnowledgeChunk b = chunk(2L, "B", "退款规则", "[0.0,1.0]");
        when(chunkMapper.selectByKeyword("USER", query, 20))
                .thenReturn(List.of(withScore(b, 5.0)));
        when(rerankClient.rerank(eq(query), anyList(), eq(4)))
                .thenReturn(List.of(new RerankResult(0, 0.90)));

        KnowledgeContext context = retriever.retrieve(AiScope.USER, query);

        assertThat(context.snippets()).hasSize(1);
        assertThat(context.snippets().getFirst().title()).isEqualTo("B");
        verify(chunkMapper, never()).selectVectorCandidates(anyString(), eq(200));
    }

    @Test
    @DisplayName("结果缓存命中时不访问数据库与模型")
    void retrieve_resultCacheHit() throws Exception {
        String query = "订单取消";
        String key = RedisKey.aiRagResult(
                "USER", TextHasher.sha256(TextHasher.normalize(query)));
        KnowledgeContext cached = new KnowledgeContext(List.of(
                new KnowledgeSnippet("A", "章节", "缓存内容", 0.9)));
        when(valueOps.get(key)).thenReturn(objectMapper.writeValueAsString(cached));

        KnowledgeContext context = retriever.retrieve(AiScope.USER, query);

        assertThat(context.snippets()).hasSize(1);
        assertThat(context.snippets().getFirst().title()).isEqualTo("A");
        verifyNoInteractions(chunkMapper, embeddingClient, rerankClient);
    }

    @Test
    @DisplayName("RAG 关闭时直接返回空")
    void retrieve_disabled() {
        properties.setEnabled(false);

        assertThat(retriever.retrieve(AiScope.USER, "订单取消").isEmpty()).isTrue();
        verifyNoInteractions(chunkMapper, embeddingClient, rerankClient, redis);
    }

    private KnowledgeChunk chunk(Long id, String title, String content, String embedding) {
        KnowledgeChunk chunk = new KnowledgeChunk();
        chunk.setId(id);
        chunk.setTitle(title);
        chunk.setSection("章节");
        chunk.setContent(content);
        chunk.setEmbedding(embedding);
        chunk.setContentHash("hash-" + id);
        chunk.setEnabled(1);
        return chunk;
    }

    private KnowledgeChunk withScore(KnowledgeChunk chunk, double score) {
        chunk.setScore(score);
        return chunk;
    }
}
