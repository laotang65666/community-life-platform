package com.laotang.core.ai.tool;

/**
 * 一次工具调用的审计记录,最终序列化进 {@code tb_ai_message.tool_calls}。
 *
 * @param tool      工具名
 * @param arguments 调用参数摘要(不含用户身份)
 * @param success   是否成功
 * @param message   成功为 ok,失败为原因
 * @param elapsedMs 耗时
 */
public record ToolInvocation(
        String tool,
        String arguments,
        boolean success,
        String message,
        long elapsedMs
) {
}
