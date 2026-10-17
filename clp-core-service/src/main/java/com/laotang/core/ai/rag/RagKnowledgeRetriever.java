package com.laotang.core.ai.rag;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.laotang.common.constant.RedisKey;
import com.laotang.common.exception.BusinessException;
import com.laotang.core.ai.enums.AiScope;
import com.laotang.core.ai.rag.config.RagProperties;
import com.laotang.core.ai.rag.entity.KnowledgeChunk;
import com.laotang.core.ai.rag.mapper.KnowledgeChunkMapper;
import com.laotang.framework.ai.zhipu.EmbeddingClient;
import com.laotang.framework.ai.zhipu.RerankClient;
import com.laotang.framework.ai.zhipu.RerankResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 混合检索实现:向量召回 + 关键词召回 → RRF 融合 → rerank 精排 → 证据过滤。
 *
 * <p>降级策略:</p>
 * <ul>
 *   <li>embedding 不可用 → 只走关键词召回;</li>
 *   <li>全文索引不可用 → 关键词通道退化为 LIKE;</li>
 *   <li>rerank 不可用 → 使用 RRF 排序结果。</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RagKnowledgeRetriever implements KnowledgeRetriever {

    private final KnowledgeChunkMapper chunkMapper;
    private final EmbeddingClient embeddingClient;
    private final RerankClient rerankClient;
    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;
    private final RagProperties properties;
    private final KnowledgeVectorCache vectorCache;

    @Override
    public KnowledgeContext retrieve(AiScope scope, String query) {
        try {
            return doRetrieve(scope, query);
        } catch (Exception ex) {
            // 检索是增强能力,任何异常都不能影响正常对话
            log.warn("RAG 检索失败,本轮不使用参考资料: {}", ex.getMessage(), ex);
            return KnowledgeContext.empty();
        }
    }

    private KnowledgeContext doRetrieve(AiScope scope, String query) {
        if (!properties.isEnabled() || query == null || query.isBlank()) {
            return KnowledgeContext.empty();
        }

        String normalized = TextHasher.normalize(query);
        String queryHash = TextHasher.sha256(normalized);
        String cacheKey = RedisKey.aiRagResult(scope.name(), queryHash);

        KnowledgeContext cached = readCache(cacheKey);
        if (cached != null) {
            return cached;
        }

        List<RankedChunk> vectorHits = vectorRecall(scope, normalized);
        List<RankedChunk> keywordHits = keywordRecall(scope, normalized);
        List<RankedChunk> fused = fuse(vectorHits, keywordHits);

        KnowledgeContext context = fused.isEmpty()
                ? KnowledgeContext.empty()
                : buildContext(rerank(normalized, fused));

        writeCache(cacheKey, context);
        log.debug("RAG 检索完成: scope={}, query={}, vector={}, keyword={}, final={}",
                scope, normalized, vectorHits.size(), keywordHits.size(), context.snippets().size());
        return context;
    }

    private List<RankedChunk> vectorRecall(AiScope scope, String query) {
        float[] queryVector;
        try {
            queryVector = queryEmbedding(query);
        } catch (BusinessException ex) {
            log.warn("向量召回不可用,仅使用关键词召回: {}", ex.getMessage());
            return List.of();
        }

        List<KnowledgeChunk> chunks = chunkMapper.selectVectorCandidates(
                scope.name(), properties.getVectorCandidates());

        List<RankedChunk> scored = new ArrayList<>(chunks.size());
        for (KnowledgeChunk chunk : chunks) {
            float[] vector = vectorCache.get(chunk);
            if (vector == null) {
                continue;
            }
            scored.add(new RankedChunk(chunk, VectorMath.cosine(queryVector, vector)));
        }
        scored.sort(Comparator.comparingDouble(RankedChunk::score).reversed());
        return scored;
    }

    private List<RankedChunk> keywordRecall(AiScope scope, String query) {
        try {
            return chunkMapper.selectByKeyword(
                            scope.name(), query, properties.getKeywordCandidates())
                    .stream()
                    .map(chunk -> new RankedChunk(
                            chunk, chunk.getScore() == null ? 0.0 : chunk.getScore()))
                    .toList();
        } catch (DataAccessException ex) {
            log.warn("全文检索不可用,尝试 LIKE 兜底: {}", ex.getMessage());
        }

        try {
            return chunkMapper.selectByLike(
                            scope.name(), query, properties.getKeywordCandidates())
                    .stream()
                    .map(chunk -> new RankedChunk(chunk, 0.0))
                    .toList();
        } catch (DataAccessException ex) {
            log.warn("关键词召回失败,本轮仅使用向量召回: {}", ex.getMessage());
            return List.of();
        }
    }

    /**
     * Reciprocal Rank Fusion:
     * {@code score = Σ 1 / (k + rank)},rank 从 1 开始。
     *
     * <p>只看名次不看原始分数,避免把余弦相似度和全文检索分数直接相加。</p>
     */
    private List<RankedChunk> fuse(List<RankedChunk> vectorHits, List<RankedChunk> keywordHits) {
        int k = properties.getRrfK();
        Map<Long, KnowledgeChunk> chunks = new LinkedHashMap<>();
        Map<Long, Double> scores = new HashMap<>();

        for (int i = 0; i < vectorHits.size(); i++) {
            RankedChunk hit = vectorHits.get(i);
            chunks.putIfAbsent(hit.chunk().getId(), hit.chunk());
            scores.merge(hit.chunk().getId(), 1.0 / (k + i + 1), Double::sum);
        }
        for (int i = 0; i < keywordHits.size(); i++) {
            RankedChunk hit = keywordHits.get(i);
            chunks.putIfAbsent(hit.chunk().getId(), hit.chunk());
            scores.merge(hit.chunk().getId(), 1.0 / (k + i + 1), Double::sum);
        }

        return scores.entrySet().stream()
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                .limit(properties.getFusionTopN())
                .map(entry -> new RankedChunk(chunks.get(entry.getKey()), entry.getValue()))
                .toList();
    }

    private List<RankedChunk> rerank(String query, List<RankedChunk> fused) {
        List<String> documents = fused.stream().map(this::rerankText).toList();
        try {
            List<RerankResult> results = rerankClient.rerank(
                    query, documents, properties.getRerankTopN());

            List<RankedChunk> kept = new ArrayList<>();
            for (RerankResult result : results) {
                if (result.index() < 0 || result.index() >= fused.size()) {
                    continue;
                }
                if (result.score() < properties.getMinScore()) {
                    continue;
                }
                kept.add(new RankedChunk(fused.get(result.index()).chunk(), result.score()));
            }
            kept.sort(Comparator.comparingDouble(RankedChunk::score).reversed());
            return kept;
        } catch (BusinessException ex) {
            log.warn("精排不可用,降级为 RRF 排序: {}", ex.getMessage());
            return fused.stream().limit(properties.getRerankTopN()).toList();
        }
    }

    private KnowledgeContext buildContext(List<RankedChunk> ranked) {
        List<KnowledgeSnippet> snippets = new ArrayList<>();
        int usedChars = 0;
        int maxChars = properties.getMaxContextChars();

        for (RankedChunk hit : ranked) {
            String content = hit.chunk().getContent();
            if (content == null || content.isBlank()) {
                continue;
            }
            if (usedChars + content.length() > maxChars) {
                if (snippets.isEmpty()) {
                    content = content.substring(0, Math.min(content.length(), maxChars));
                } else {
                    break;
                }
            }
            snippets.add(new KnowledgeSnippet(
                    hit.chunk().getDocId(),
                    hit.chunk().getTitle(),
                    hit.chunk().getSection(),
                    content,
                    hit.score()));
            usedChars += content.length();
        }
        return new KnowledgeContext(snippets);
    }

    private float[] queryEmbedding(String query) {
        String key = RedisKey.aiQueryEmbedding(TextHasher.sha256(query));
        try {
            String cached = redis.opsForValue().get(key);
            if (cached != null) {
                return objectMapper.readValue(cached, float[].class);
            }
        } catch (Exception ex) {
            log.warn("读取查询向量缓存失败: {}", ex.getMessage());
        }

        float[] vector = embeddingClient.embed(query);
        try {
            redis.opsForValue().set(
                    key, objectMapper.writeValueAsString(vector),
                    properties.getQueryEmbeddingTtl());
        } catch (Exception ex) {
            log.warn("写入查询向量缓存失败: {}", ex.getMessage());
        }
        return vector;
    }

    private KnowledgeContext readCache(String key) {
        try {
            String json = redis.opsForValue().get(key);
            return json == null ? null : objectMapper.readValue(json, KnowledgeContext.class);
        } catch (Exception ex) {
            log.warn("读取 RAG 结果缓存失败: {}", ex.getMessage());
            return null;
        }
    }

    private void writeCache(String key, KnowledgeContext context) {
        try {
            redis.opsForValue().set(
                    key, objectMapper.writeValueAsString(context), properties.getResultTtl());
        } catch (Exception ex) {
            log.warn("写入 RAG 结果缓存失败: {}", ex.getMessage());
        }
    }

    private String rerankText(RankedChunk hit) {
        KnowledgeChunk chunk = hit.chunk();
        StringBuilder builder = new StringBuilder(chunk.getTitle()).append('\n');
        if (chunk.getSection() != null && !chunk.getSection().isBlank()) {
            builder.append(chunk.getSection()).append('\n');
        }
        builder.append(chunk.getContent());
        return builder.toString();
    }

    private record RankedChunk(KnowledgeChunk chunk, double score) {
    }
}
