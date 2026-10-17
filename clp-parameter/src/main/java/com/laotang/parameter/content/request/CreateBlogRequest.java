package com.laotang.parameter.content.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 发布笔记。
 */
public record CreateBlogRequest(

        @NotBlank(message = "标题不能为空")
        @Size(max = 128, message = "标题不能超过 128 个字符")
        String title,

        @NotBlank(message = "正文不能为空")
        String content,

        @Size(max = 9, message = "最多上传 9 张图片")
        List<@Size(max = 500, message = "图片 URL 不能超过 500 个字符") String> images,

        Long shopId
) {
}
