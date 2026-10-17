package com.laotang.framework.ai.zhipu;

import java.util.List;

/**
 * 文本向量化能力。
 *
 * <p>接口与具体厂商解耦,RAG 层只依赖这个接口;将来换模型或自建向量服务
 * 不需要改检索逻辑。</p>
 */
public interface EmbeddingClient {

    /**
     * 单条文本向量化。
     */
    float[] embed(String text);

    /**
     * 批量向量化,顺序与入参一致。
     */
    List<float[]> embedAll(List<String> texts);

    /**
     * 当前向量维度。
     */
    int dimensions();
}
