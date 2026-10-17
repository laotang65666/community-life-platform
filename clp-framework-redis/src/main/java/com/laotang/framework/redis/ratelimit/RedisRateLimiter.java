package com.laotang.framework.redis.ratelimit;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 基于 Redis 的固定窗口限流。
 *
 * <p>INCR 计数,第一次设置过期时间;窗口内超过阈值即拒绝。
 * 固定窗口实现简单,代价是窗口边界可能有短暂的双倍流量,
 * 秒杀入口这种"宁可多拦一点"的场景够用了。</p>
 */
@Component
@RequiredArgsConstructor
public class RedisRateLimiter {

    private final StringRedisTemplate redis;

    public boolean tryAcquire(String key, int limit, Duration window) {
        Long count = redis.opsForValue().increment(key);
        if (count != null && count == 1L) {
            redis.expire(key, window);
        }
        return count != null && count <= limit;
    }
}
