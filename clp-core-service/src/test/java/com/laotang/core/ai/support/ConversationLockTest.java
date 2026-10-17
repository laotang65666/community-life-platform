package com.laotang.core.ai.support;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConversationLockTest {

    @Mock
    private RedissonClient redisson;
    @Mock
    private RLock lock;

    private ConversationLock conversationLock;

    @BeforeEach
    void setUp() {
        conversationLock = new ConversationLock(redisson);
        when(redisson.getLock("clp:ai:conversation:lock:100")).thenReturn(lock);
    }

    @Test
    @DisplayName("抢到锁返回 true")
    void tryLock_success() throws InterruptedException {
        when(lock.tryLock(0, 60, TimeUnit.SECONDS)).thenReturn(true);

        assertThat(conversationLock.tryLock(100L)).isTrue();
    }

    @Test
    @DisplayName("锁被占用返回 false")
    void tryLock_busy() throws InterruptedException {
        when(lock.tryLock(0, 60, TimeUnit.SECONDS)).thenReturn(false);

        assertThat(conversationLock.tryLock(100L)).isFalse();
    }

    @Test
    @DisplayName("只有当前线程持有时才释放")
    void unlock_onlyWhenHeld() {
        when(lock.isHeldByCurrentThread()).thenReturn(true);

        conversationLock.unlock(100L);

        verify(lock).unlock();
    }

    @Test
    @DisplayName("未持有时不释放")
    void unlock_notHeld() {
        when(lock.isHeldByCurrentThread()).thenReturn(false);

        conversationLock.unlock(100L);

        verify(lock, never()).unlock();
    }
}
