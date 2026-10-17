package com.laotang.framework.ai.zhipu;

import java.util.List;

/**
 * 检索结果重排能力。
 */
public interface RerankClient {

    /**
     * 按与 query 的相关性对候选文档重排。
     *
     * @param query     用户问题
     * @param documents 候选文档正文
     * @param topN      最多返回条数
     * @return 按相关性降序,{@code index} 指向入参 documents 的下标
     */
    List<RerankResult> rerank(String query, List<String> documents, int topN);
}
