package com.laotang.parameter.ai.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 管理员导入知识文档。
 *
 * @param scope USER / MERCHANT / ALL
 */
public record IngestKnowledgeRequest(

        @NotBlank(message = "docId 不能为空")
        @Size(max = 64, message = "docId 不能超过 64 个字符")
        String docId,

        @NotBlank(message = "scope 不能为空")
        String scope,

        @NotBlank(message = "标题不能为空")
        @Size(max = 255, message = "标题不能超过 255 个字符")
        String title,

        @NotBlank(message = "正文不能为空")
        String content
) {
}
