package com.laotang.core.idempotency.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * 基于 Redis 的幂等快路径。
 *
 * <p>秒杀这种超高 QPS 场景,如果每个请求都先写一次数据库幂等表,
 * 数据库会成为瓶颈。这里用 Redis SETNX 做前置去重,
 * 最终正确性仍由数据库唯一索引兜底。</p>
 */
@Service
@RequiredArgsConstructor
public class RedisIdempotencyService {

    public static final String PROCESSING = "PROCESSING";

    private final StringRedisTemplate redis;

    /**
     * 抢占幂等键。成功返回 true,已被占用返回 false。
     */
    public boolean tryAcquire(String key, Duration ttl) {
        return Boolean.TRUE.equals(redis.opsForValue().setIfAbsent(key, PROCESSING, ttl));
    }

    public String get(String key) {
        return redis.opsForValue().get(key);
    }

    /**
     * 记录业务结果。之后同一个 Key 再来,直接返回这个结果。
     */
    public void markSuccess(String key, String bizId, Duration ttl) {
        redis.opsForValue().set(key, bizId, ttl);
    }

    public void release(String key) {
        redis.delete(key);
    }
}
