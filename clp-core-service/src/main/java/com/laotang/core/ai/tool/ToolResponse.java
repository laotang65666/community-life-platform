package com.laotang.core.ai.tool;

/**
 * 工具统一返回结构。
 *
 * <p>工具不向模型抛异常,而是返回 {@code success=false} + 可读信息,
 * 让模型能向用户解释"查不到"而不是直接中断整轮对话。</p>
 */
public record ToolResponse<T>(boolean success, T data, String message) {

    public static <T> ToolResponse<T> ok(T data) {
        return new ToolResponse<>(true, data, "ok");
    }

    public static <T> ToolResponse<T> fail(String message) {
        return new ToolResponse<>(false, null, message);
    }
}
