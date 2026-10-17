package com.laotang.parameter.ai.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * 一条 RAG 评估用例。
 *
 * @param question        测试问题
 * @param expectedDocIds  期望命中的文档 ID,命中任一即算正确
 */
public record RagEvalCase(

        @NotBlank(message = "问题不能为空")
        String question,

        @NotEmpty(message = "expectedDocIds 不能为空")
        List<String> expectedDocIds
) {
}
