package com.laotang.core.ai.rag;

import com.laotang.core.ai.enums.AiScope;

/**
 * 知识检索。
 */
public interface KnowledgeRetriever {

    /**
     * 按助手范围检索与问题相关的知识。
     *
     * <p>实现必须保证降级可用:向量、全文索引、精排任一环节失败时,
     * 应返回其余环节的结果,而不是让整轮对话失败。</p>
     */
    KnowledgeContext retrieve(AiScope scope, String query);
}
