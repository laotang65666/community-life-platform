package com.laotang.core.ai.rag.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * RAG 检索参数。
 */
@Data
@ConfigurationProperties(prefix = "clp.ai.rag")
public class RagProperties {

    private boolean enabled = true;

    /**
     * 启动时导入 classpath:knowledge/*.md 种子知识。
     */
    private boolean seedEnabled = true;

    /**
     * 单个分片的最大字符数。
     */
    private int maxChunkChars = 600;

    /**
     * 向量召回候选数。
     */
    private int vectorCandidates = 200;

    /**
     * 关键词召回候选数。
     */
    private int keywordCandidates = 20;

    /**
     * RRF 常数,经验值 60。
     */
    private int rrfK = 60;

    /**
     * 融合后送去精排的条数。
     */
    private int fusionTopN = 10;

    /**
     * 精排后保留的条数。
     */
    private int rerankTopN = 4;

    /**
     * 精排证据阈值,低于该分数视为无关。
     */
    private double minScore = 0.35;

    /**
     * 拼进提示词的参考资料总长度上限。
     */
    private int maxContextChars = 2000;

    /**
     * 查询向量缓存时间。
     */
    private Duration queryEmbeddingTtl = Duration.ofHours(1);

    /**
     * 检索结果缓存时间。
     */
    private Duration resultTtl = Duration.ofMinutes(5);

    /**
     * 进程内向量缓存的最大分片数。
     */
    private int vectorCacheMax = 5000;
}
