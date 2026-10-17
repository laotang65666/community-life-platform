package com.laotang.core.ai.rag;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.laotang.core.ai.rag.config.RagProperties;
import com.laotang.core.ai.rag.entity.KnowledgeChunk;
import com.laotang.core.ai.rag.mapper.KnowledgeChunkMapper;
import com.laotang.framework.ai.zhipu.EmbeddingClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 知识入库。
 *
 * <p>按内容哈希增量写入:同一文档重复导入只补齐新增分片,
 * 已删除的分片同步清理,避免全量重算 embedding。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeIngestionService {

    private static final int EMBED_BATCH_SIZE = 16;

    private final KnowledgeChunkMapper chunkMapper;
    private final EmbeddingClient embeddingClient;
    private final KnowledgeChunker chunker;
    private final KnowledgeVectorCache vectorCache;
    private final RagProperties properties;
    private final ObjectMapper objectMapper;

    /**
     * 刻意不加事务:embedding 是远程调用,可能持续数秒,
     * 包在事务里会长时间占用数据库连接。分片按哈希幂等,
     * 中途失败后重试只会补齐缺失部分。
     */
    public IngestResult ingest(
            String docId, KnowledgeScope scope, String title, String content) {

        List<KnowledgeChunker.ChunkDraft> drafts = chunker.split(title, content);
        if (drafts.isEmpty()) {
            return new IngestResult(docId, 0, 0, 0);
        }

        List<KnowledgeChunk> existing = chunkMapper.selectByDocId(docId);
        Set<String> existingHashes = new HashSet<>();
        existing.forEach(chunk -> existingHashes.add(chunk.getContentHash()));

        Set<String> currentHashes = new HashSet<>();
        drafts.forEach(draft -> currentHashes.add(draft.hash()));

        List<KnowledgeChunker.ChunkDraft> missing = drafts.stream()
                .filter(draft -> !existingHashes.contains(draft.hash()))
                .toList();

        int inserted = embedAndInsert(docId, scope, title, missing);
        int deleted = deleteStale(existing, currentHashes);

        if (inserted > 0 || deleted > 0) {
            vectorCache.clear();
        }
        log.info("知识入库完成: docId={}, scope={}, total={}, inserted={}, deleted={}",
                docId, scope, drafts.size(), inserted, deleted);
        return new IngestResult(docId, drafts.size(), inserted, deleted);
    }

    @Transactional
    public int deleteDoc(String docId) {
        List<KnowledgeChunk> existing = chunkMapper.selectByDocId(docId);
        existing.forEach(chunk -> chunkMapper.deleteById(chunk.getId()));
        vectorCache.clear();
        return existing.size();
    }

    private int embedAndInsert(
            String docId, KnowledgeScope scope, String title,
            List<KnowledgeChunker.ChunkDraft> missing) {

        int inserted = 0;
        for (int from = 0; from < missing.size(); from += EMBED_BATCH_SIZE) {
            int to = Math.min(from + EMBED_BATCH_SIZE, missing.size());
            List<KnowledgeChunker.ChunkDraft> batch = missing.subList(from, to);

            List<String> texts = batch.stream()
                    .map(draft -> embeddingText(title, draft))
                    .toList();
            List<float[]> vectors = embeddingClient.embedAll(texts);
            if (vectors.size() != batch.size()) {
                throw new IllegalStateException("向量数量与分片数量不一致");
            }

            for (int i = 0; i < batch.size(); i++) {
                KnowledgeChunker.ChunkDraft draft = batch.get(i);
                KnowledgeChunk chunk = new KnowledgeChunk();
                chunk.setDocId(docId);
                chunk.setScope(scope.name());
                chunk.setTitle(title);
                chunk.setSection(draft.section());
                chunk.setContent(draft.content());
                chunk.setEmbedding(toJson(vectors.get(i)));
                chunk.setDimension(embeddingClient.dimensions());
                chunk.setContentHash(draft.hash());
                chunk.setEnabled(1);
                chunk.setCreatedAt(LocalDateTime.now());
                chunk.setUpdatedAt(LocalDateTime.now());
                chunkMapper.insert(chunk);
                inserted++;
            }
        }
        return inserted;
    }

    private int deleteStale(List<KnowledgeChunk> existing, Set<String> currentHashes) {
        int deleted = 0;
        for (KnowledgeChunk chunk : existing) {
            if (!currentHashes.contains(chunk.getContentHash())) {
                chunkMapper.deleteById(chunk.getId());
                deleted++;
            }
        }
        return deleted;
    }

    private String embeddingText(String title, KnowledgeChunker.ChunkDraft draft) {
        List<String> parts = new ArrayList<>(3);
        parts.add(title);
        if (draft.section() != null && !draft.section().isBlank()) {
            parts.add(draft.section());
        }
        parts.add(draft.content());
        return String.join("\n", parts);
    }

    private String toJson(float[] vector) {
        try {
            return objectMapper.writeValueAsString(vector);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("序列化向量失败", ex);
        }
    }

    /**
     * 入库结果。
     */
    public record IngestResult(
            String docId,
            int totalChunks,
            int insertedChunks,
            int deletedChunks
    ) {
    }
}
