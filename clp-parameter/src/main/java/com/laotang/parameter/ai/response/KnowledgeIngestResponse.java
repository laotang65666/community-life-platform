package com.laotang.parameter.ai.response;

/**
 * 知识导入结果。
 */
public record KnowledgeIngestResponse(
        String docId,
        int totalChunks,
        int insertedChunks,
        int deletedChunks
) {
}
