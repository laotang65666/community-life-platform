package com.laotang.core.ai.rag;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.laotang.core.ai.rag.config.RagProperties;
import com.laotang.core.ai.rag.entity.KnowledgeChunk;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 分片向量的进程内缓存。
 *
 * <p>数据库里的 JSON 向量每次请求都反序列化会很浪费;这里按
 * {@code chunkId + contentHash} 缓存,知识更新时整体清空。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KnowledgeVectorCache {

    private final ObjectMapper objectMapper;
    private final RagProperties properties;

    private final ConcurrentHashMap<Long, CachedVector> cache = new ConcurrentHashMap<>();

    public float[] get(KnowledgeChunk chunk) {
        String version = chunk.getId() + ":" + chunk.getContentHash();
        CachedVector cached = cache.get(chunk.getId());
        if (cached != null && cached.version().equals(version)) {
            return cached.vector();
        }

        float[] vector = parse(chunk.getEmbedding());
        if (vector == null) {
            return null;
        }
        if (cache.size() >= properties.getVectorCacheMax()) {
            cache.clear();
        }
        cache.put(chunk.getId(), new CachedVector(version, vector));
        return vector;
    }

    public void clear() {
        cache.clear();
    }

    private float[] parse(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, float[].class);
        } catch (Exception ex) {
            log.warn("解析知识分片向量失败", ex);
            return null;
        }
    }

    private record CachedVector(String version, float[] vector) {
    }
}
