package com.laotang.core.ai.rag;

/**
 * 一条命中证据。
 *
 * @param docId   文档 ID,用于引用与离线评估
 * @param title   文档标题
 * @param section 章节
 * @param content 证据正文
 * @param score   精排分数
 */
public record KnowledgeSnippet(
        String docId,
        String title,
        String section,
        String content,
        double score
) {
}
