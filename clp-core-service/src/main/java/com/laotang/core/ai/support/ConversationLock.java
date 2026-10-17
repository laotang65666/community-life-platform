package com.laotang.core.ai.support;

import com.laotang.common.constant.RedisKey;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 会话级分布式锁。
 *
 * <p>同一会话的并发请求会导致历史交错、摘要重复、消息顺序错乱,
 * 这里用 Redisson 锁把单会话的处理串行化。锁粒度为会话,不影响不同会话。</p>
 */
@Component
@RequiredArgsConstructor
public class ConversationLock {

    private static final long LEASE_SECONDS = 60;

    private final RedissonClient redisson;

    public boolean tryLock(Long conversationId) {
        try {
            return lock(conversationId).tryLock(0, LEASE_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    public void unlock(Long conversationId) {
        RLock lock = lock(conversationId);
        try {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        } catch (IllegalMonitorStateException ex) {
            // 租约到期后锁可能已被释放,忽略即可
        }
    }

    private RLock lock(Long conversationId) {
        return redisson.getLock(RedisKey.aiConversationLock(conversationId));
    }
}
