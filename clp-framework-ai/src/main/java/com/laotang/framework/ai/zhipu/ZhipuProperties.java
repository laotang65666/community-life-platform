package com.laotang.framework.ai.zhipu;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 智谱开放平台的接入配置。
 *
 * <p>只放非敏感项与 API Key 的占位;真实 Key 由 {@code application-local.yml}
 * 或环境变量 {@code ZHIPU_API_KEY} 提供,不入库。</p>
 */
@Data
@ConfigurationProperties(prefix = "clp.ai.zhipu")
public class ZhipuProperties {

    /**
     * 开放平台地址,注意不带 {@code /api/paas/v4}。
     */
    private String baseUrl = "https://open.bigmodel.cn";

    /**
     * 形如 {@code {id}.{secret}},必须是完整 Key。
     */
    private String apiKey = "";

    private String embeddingModel = "embedding-3";

    /**
     * rerank-pro 的分数更有区分度,适合做证据阈值过滤;
     * 普通 rerank 对短文本常返回接近 1 的分数。
     */
    private String rerankModel = "rerank-pro";

    /**
     * embedding-3 支持自定义维度,这里统一 1024,与数据库向量长度保持一致。
     */
    private int embeddingDimensions = 1024;

    private int connectTimeoutMs = 3000;

    private int readTimeoutMs = 10000;
}
