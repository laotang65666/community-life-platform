package com.laotang.parameter.ai.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * RAG 检索效果评估请求。
 *
 * @param scope USER / MERCHANT
 * @param cases 评估用例
 */
public record RagEvalRequest(

        @NotBlank(message = "scope 不能为空")
        String scope,

        @NotEmpty(message = "cases 不能为空")
        @Valid
        List<RagEvalCase> cases
) {
}
