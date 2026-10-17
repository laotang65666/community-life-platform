package com.laotang.core.ai.controller;

import com.laotang.common.annotation.RequireRole;
import com.laotang.common.enums.RoleEnum;
import com.laotang.common.result.Result;
import com.laotang.core.ai.enums.AiScope;
import com.laotang.core.ai.rag.RagEvaluationService;
import com.laotang.parameter.ai.request.RagEvalRequest;
import com.laotang.parameter.ai.response.RagEvalResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理后台:RAG 检索效果评估。
 */
@Tag(name = "管理后台 - RAG 评估")
@RestController
@RequestMapping("/api/v1/admin/ai/rag")
@RequiredArgsConstructor
@RequireRole(RoleEnum.ADMIN)
public class AdminRagController {

    private final RagEvaluationService evaluationService;

    @Operation(summary = "检索效果评估(HitRate / MRR)")
    @PostMapping("/evaluate")
    public Result<RagEvalResponse> evaluate(@Valid @RequestBody RagEvalRequest request) {
        return Result.ok(evaluationService.evaluate(
                AiScope.from(request.scope()), request.cases()));
    }
}
