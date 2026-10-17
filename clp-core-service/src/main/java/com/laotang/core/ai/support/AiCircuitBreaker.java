package com.laotang.core.ai.support;

import com.laotang.core.ai.config.AiChatProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.LongSupplier;

/**
 * 轻量熔断器。
 *
 * <p>状态机:CLOSED → (连续失败达到阈值) → OPEN → (冷却结束) → HALF_OPEN
 * → 探测成功回到 CLOSED,探测失败重新 OPEN。</p>
 *
 * <p>进程内实现,单机有效;多节点各自维护状态。没有引入 Resilience4j,
 * 是因为这里只需要"连续失败 + 冷却 + 半开探测"三个能力。</p>
 */
@Slf4j
@Component
public class AiCircuitBreaker {

    private enum State {
        CLOSED, OPEN, HALF_OPEN
    }

    private final AiChatProperties properties;
    private final LongSupplier clock;

    private final AtomicReference<State> state = new AtomicReference<>(State.CLOSED);
    private final AtomicInteger consecutiveFailures = new AtomicInteger();
    private final AtomicLong openUntil = new AtomicLong();
    private final AtomicBoolean probeInFlight = new AtomicBoolean();

    @Autowired
    public AiCircuitBreaker(AiChatProperties properties) {
        this(properties, System::currentTimeMillis);
    }

    AiCircuitBreaker(AiChatProperties properties, LongSupplier clock) {
        this.properties = properties;
        this.clock = clock;
    }

    /**
     * 是否允许本次请求通过。OPEN 且未到冷却时间时拒绝。
     */
    public boolean allowRequest() {
        State current = state.get();
        if (current == State.CLOSED) {
            return true;
        }
        if (current == State.OPEN) {
            if (clock.getAsLong() < openUntil.get()) {
                return false;
            }
            if (state.compareAndSet(State.OPEN, State.HALF_OPEN)) {
                probeInFlight.set(true);
                return true;
            }
            return false;
        }
        // HALF_OPEN: 只放一个探测请求
        return probeInFlight.compareAndSet(false, true);
    }

    public void onSuccess() {
        consecutiveFailures.set(0);
        probeInFlight.set(false);
        state.set(State.CLOSED);
    }

    public void onFailure() {
        probeInFlight.set(false);
        int failures = consecutiveFailures.incrementAndGet();
        boolean halfOpen = state.get() == State.HALF_OPEN;

        if (halfOpen || failures >= properties.getCircuitFailureThreshold()) {
            state.set(State.OPEN);
            openUntil.set(clock.getAsLong() + properties.getCircuitOpenMs());
            consecutiveFailures.set(0);
            log.warn("AI 熔断器打开,{}ms 内请求直接降级", properties.getCircuitOpenMs());
        }
    }

    /**
     * 当前是否处于打开状态(尚未进入冷却后的半开窗口)。
     */
    public boolean isOpen() {
        return state.get() == State.OPEN && clock.getAsLong() < openUntil.get();
    }
}
