package com.laotang.core.ai.rag;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.laotang.core.ai.rag.config.RagProperties;
import com.laotang.core.ai.rag.entity.KnowledgeChunk;
import com.laotang.core.ai.rag.mapper.KnowledgeChunkMapper;
import com.laotang.framework.ai.zhipu.EmbeddingClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KnowledgeIngestionServiceTest {

    @Mock
    private KnowledgeChunkMapper chunkMapper;
    @Mock
    private EmbeddingClient embeddingClient;
    @Mock
    private KnowledgeVectorCache vectorCache;

    private RagProperties properties;
    private KnowledgeChunker chunker;
    private KnowledgeIngestionService service;

    @BeforeEach
    void setUp() {
        properties = new RagProperties();
        properties.setMaxChunkChars(50);
        chunker = new KnowledgeChunker(properties);
        service = new KnowledgeIngestionService(
                chunkMapper, embeddingClient, chunker, vectorCache,
                properties, new ObjectMapper());
    }

    private String content() {
        return """
                ## 下单
                用户只能对同一店铺的商品统一下单。
                ## 退款
                平台首期未开放线上退款流程。
                """;
    }

    @Test
    @DisplayName("新文档:每个分片生成向量并入库")
    void ingest_newDoc() {
        when(chunkMapper.selectByDocId("doc")).thenReturn(List.of());
        when(embeddingClient.embedAll(anyList()))
                .thenReturn(List.of(new float[]{1, 0}, new float[]{0, 1}));
        when(embeddingClient.dimensions()).thenReturn(1024);
        when(chunkMapper.insert(any(KnowledgeChunk.class))).thenReturn(1);

        var result = service.ingest("doc", KnowledgeScope.USER, "订单规则", content());

        assertThat(result.totalChunks()).isEqualTo(2);
        assertThat(result.insertedChunks()).isEqualTo(2);
        assertThat(result.deletedChunks()).isZero();
        verify(chunkMapper, times(2)).insert(any(KnowledgeChunk.class));
        verify(vectorCache).clear();
    }

    @Test
    @DisplayName("内容未变化:跳过向量化")
    void ingest_sameContentSkipsEmbedding() {
        List<KnowledgeChunker.ChunkDraft> drafts = chunker.split("订单规则", content());
        List<KnowledgeChunk> existing = drafts.stream()
                .map(draft -> chunk(draft.hash()))
                .toList();
        when(chunkMapper.selectByDocId("doc")).thenReturn(existing);

        var result = service.ingest("doc", KnowledgeScope.USER, "订单规则", content());

        assertThat(result.insertedChunks()).isZero();
        assertThat(result.deletedChunks()).isZero();
        verify(embeddingClient, never()).embedAll(anyList());
        verify(chunkMapper, never()).insert(any(KnowledgeChunk.class));
    }

    @Test
    @DisplayName("内容变更:删除已不存在的旧分片")
    void ingest_deletesStaleChunks() {
        KnowledgeChunk stale = chunk("old-hash");
        stale.setId(999L);
        when(chunkMapper.selectByDocId("doc")).thenReturn(List.of(stale));
        when(embeddingClient.embedAll(anyList()))
                .thenReturn(List.of(new float[]{1, 0}, new float[]{0, 1}));
        when(embeddingClient.dimensions()).thenReturn(1024);
        when(chunkMapper.insert(any(KnowledgeChunk.class))).thenReturn(1);

        var result = service.ingest("doc", KnowledgeScope.USER, "订单规则", content());

        assertThat(result.deletedChunks()).isEqualTo(1);
        verify(chunkMapper).deleteById(999L);
    }

    @Test
    @DisplayName("删除文档时清空其全部分片")
    void deleteDoc() {
        KnowledgeChunk first = chunk("h1");
        first.setId(1L);
        KnowledgeChunk second = chunk("h2");
        second.setId(2L);
        when(chunkMapper.selectByDocId("doc")).thenReturn(List.of(first, second));

        int deleted = service.deleteDoc("doc");

        assertThat(deleted).isEqualTo(2);
        verify(chunkMapper).deleteById(1L);
        verify(chunkMapper).deleteById(2L);
        verify(vectorCache).clear();
    }

    private KnowledgeChunk chunk(String hash) {
        KnowledgeChunk chunk = new KnowledgeChunk();
        chunk.setContentHash(hash);
        chunk.setDocId("doc");
        return chunk;
    }
}
