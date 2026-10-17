package com.laotang.parameter.ai.response;

/**
 * 答案引用的知识来源。
 *
 * @param docId   文档 ID
 * @param title   文档标题
 * @param section 章节
 * @param score   精排相关性分数
 */
public record SourceResponse(String docId, String title, String section, double score) {
}
