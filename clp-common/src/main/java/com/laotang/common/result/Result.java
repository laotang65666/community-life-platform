package com.laotang.common.result;

import com.laotang.common.exception.ErrorCode;
import org.slf4j.MDC;

/**
 * 统一响应体。
 *
 * @param code    0 表示成功,非 0 为业务错误码
 * @param message 人类可读信息
 * @param data    业务数据
 * @param traceId 链路追踪 ID
 */
public record Result<T>(int code, String message, T data, String traceId) {

    public static <T> Result<T> ok(T data) {
        return new Result<>(0, "ok", data, currentTraceId());
    }

    public static <T> Result<T> ok() {
        return ok(null);
    }

    public static <T> Result<T> fail(ErrorCode errorCode) {
        return fail(errorCode, errorCode.getMessage());
    }

    public static <T> Result<T> fail(ErrorCode errorCode, String message) {
        return new Result<>(errorCode.getCode(), message, null, currentTraceId());
    }

    private static String currentTraceId() {
        return MDC.get("traceId");
    }
}
