package com.laotang.core.ai.tool;

import java.util.ArrayList;
import java.util.List;

/**
 * 单次问答内的工具调用记录器。
 *
 * <p>每次请求 new 一个,不注册为 Spring Bean,避免多请求共享状态。
 * 只保留前 {@value #MAX_ENTRIES} 条,防止模型反复调用工具把消息表撑大。</p>
 */
public class AiToolRecorder {

    private static final int MAX_ENTRIES = 20;

    private final List<ToolInvocation> invocations = new ArrayList<>();

    public synchronized void record(
            String tool, String arguments, boolean success, String message, long elapsedMs) {
        if (invocations.size() >= MAX_ENTRIES) {
            return;
        }
        invocations.add(new ToolInvocation(tool, arguments, success, message, elapsedMs));
    }

    public synchronized List<ToolInvocation> calls() {
        return List.copyOf(invocations);
    }

    public synchronized boolean isEmpty() {
        return invocations.isEmpty();
    }
}
