package com.laotang.core.ai.controller;

import com.laotang.common.annotation.RequireRole;
import com.laotang.common.enums.RoleEnum;
import com.laotang.common.result.Result;
import com.laotang.core.ai.rag.KnowledgeIngestionService;
import com.laotang.core.ai.rag.KnowledgeScope;
import com.laotang.parameter.ai.request.IngestKnowledgeRequest;
import com.laotang.parameter.ai.response.KnowledgeIngestResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理后台:AI 知识库维护。
 */
@Tag(name = "管理后台 - AI 知识库")
@RestController
@RequestMapping("/api/v1/admin/ai/knowledge")
@RequiredArgsConstructor
@RequireRole(RoleEnum.ADMIN)
public class AdminKnowledgeController {

    private final KnowledgeIngestionService ingestionService;

    @Operation(summary = "导入或更新知识文档")
    @PostMapping
    public Result<KnowledgeIngestResponse> ingest(
            @Valid @RequestBody IngestKnowledgeRequest request) {

        KnowledgeIngestionService.IngestResult result = ingestionService.ingest(
                request.docId(),
                KnowledgeScope.from(request.scope()),
                request.title(),
                request.content());

        return Result.ok(new KnowledgeIngestResponse(
                result.docId(),
                result.totalChunks(),
                result.insertedChunks(),
                result.deletedChunks()));
    }

    @Operation(summary = "删除知识文档")
    @DeleteMapping("/{docId}")
    public Result<Integer> delete(@PathVariable("docId") String docId) {
        return Result.ok(ingestionService.deleteDoc(docId));
    }
}
