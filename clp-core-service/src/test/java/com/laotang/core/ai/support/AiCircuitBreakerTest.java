package com.laotang.core.ai.support;

import com.laotang.core.ai.config.AiChatProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

class AiCircuitBreakerTest {

    private final AtomicLong now = new AtomicLong(1_000_000L);
    private AiCircuitBreaker breaker;

    @BeforeEach
    void setUp() {
        AiChatProperties properties = new AiChatProperties();
        properties.setCircuitFailureThreshold(3);
        properties.setCircuitOpenMs(1000);
        breaker = new AiCircuitBreaker(properties, now::get);
    }

    @Test
    @DisplayName("连续失败达到阈值后打开熔断")
    void opensAfterThreshold() {
        assertThat(breaker.allowRequest()).isTrue();

        breaker.onFailure();
        breaker.onFailure();
        assertThat(breaker.isOpen()).isFalse();
        assertThat(breaker.allowRequest()).isTrue();

        breaker.onFailure();
        assertThat(breaker.isOpen()).isTrue();
        assertThat(breaker.allowRequest()).isFalse();
    }

    @Test
    @DisplayName("冷却结束后只放一个半开探测")
    void halfOpenAllowsSingleProbe() {
        for (int i = 0; i < 3; i++) {
            breaker.onFailure();
        }
        now.addAndGet(1000);

        assertThat(breaker.isOpen()).isFalse();
        assertThat(breaker.allowRequest()).isTrue();
        assertThat(breaker.allowRequest()).isFalse();
    }

    @Test
    @DisplayName("半开探测成功回到关闭状态")
    void probeSuccessCloses() {
        for (int i = 0; i < 3; i++) {
            breaker.onFailure();
        }
        now.addAndGet(1000);

        assertThat(breaker.allowRequest()).isTrue();
        breaker.onSuccess();

        assertThat(breaker.allowRequest()).isTrue();
        assertThat(breaker.isOpen()).isFalse();
    }

    @Test
    @DisplayName("半开探测失败重新打开")
    void probeFailureReopens() {
        for (int i = 0; i < 3; i++) {
            breaker.onFailure();
        }
        now.addAndGet(1000);

        assertThat(breaker.allowRequest()).isTrue();
        breaker.onFailure();

        assertThat(breaker.isOpen()).isTrue();
        assertThat(breaker.allowRequest()).isFalse();
    }
}
