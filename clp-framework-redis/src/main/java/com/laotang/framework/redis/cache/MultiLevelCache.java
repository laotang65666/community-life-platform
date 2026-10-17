package com.laotang.framework.redis.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.function.Supplier;

/**
 * 两级缓存:L1 Caffeine(进程内) + L2 Redis(跨节点)。
 *
 * <p>读路径:L1 → L2 → 由调用方回源 DB。</p>
 * <p>写路径:更新 DB 后调用 {@link #evict(String)},同时删除 L1 与 L2。</p>
 *
 * <p>{@link #getOrLoad} 支持空值缓存:不存在的 ID 会缓存一个空标记,
 * 短时间内重复查询直接返回,防止缓存穿透。</p>
 *
 * <p><b>一致性边界:</b>L1 是进程内的,其他节点的 L1 无法被本次删除操作影响,
 * 只能靠较短的 {@code local-ttl} 收敛;对强一致要求高的数据不要走本地缓存。</p>
 */
@Slf4j
@Component
public class MultiLevelCache {

    private static final Object NULL_SENTINEL = new Object();
    private static final String NULL_JSON = "__NULL__";

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;
    private final CacheProperties properties;
    private final Cache<String, Object> local;

    public MultiLevelCache(
            StringRedisTemplate redis,
            ObjectMapper objectMapper,
            CacheProperties properties) {
        this.redis = redis;
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.local = Caffeine.newBuilder()
                .maximumSize(properties.getMaxSize())
                .expireAfterWrite(properties.getLocalTtl())
                .build();
    }

    /**
     * 读缓存,未命中时回源并写回两级缓存。
     *
     * <p>loader 返回 null 时写入短 TTL 的空值标记,后续请求在 TTL 内
     * 不会再次回源,避免不存在的 ID 反复打数据库。</p>
     */
    public <T> T getOrLoad(String key, Class<T> type, Duration ttl, Supplier<T> loader) {
        Object localValue = local.getIfPresent(key);
        if (localValue != null) {
            return localValue == NULL_SENTINEL ? null : type.cast(localValue);
        }

        try {
            String json = redis.opsForValue().get(key);
            if (json != null) {
                if (NULL_JSON.equals(json)) {
                    local.put(key, NULL_SENTINEL);
                    return null;
                }
                T value = objectMapper.readValue(json, type);
                local.put(key, value);
                return value;
            }
        } catch (Exception ex) {
            log.warn("读取多级缓存失败,回源加载 key={}", key, ex);
        }

        T value = loader.get();
        if (value == null) {
            local.put(key, NULL_SENTINEL);
            try {
                redis.opsForValue().set(key, NULL_JSON, properties.getNullTtl());
            } catch (Exception ex) {
                log.warn("写入空值缓存失败 key={}", key, ex);
            }
            return null;
        }

        local.put(key, value);
        try {
            redis.opsForValue().set(key, objectMapper.writeValueAsString(value), ttl);
        } catch (Exception ex) {
            log.warn("写入多级缓存失败 key={}", key, ex);
        }
        return value;
    }

    /**
     * 读取缓存,未命中返回 null。
     */
    public <T> T get(String key, Class<T> type) {
        Object localValue = local.getIfPresent(key);
        if (localValue != null) {
            return type.cast(localValue);
        }

        try {
            String json = redis.opsForValue().get(key);
            if (json == null) {
                return null;
            }
            T value = objectMapper.readValue(json, type);
            local.put(key, value);
            return value;
        } catch (Exception ex) {
            // 缓存故障不能影响主链路,当作未命中处理
            log.warn("读取多级缓存失败 key={}", key, ex);
            return null;
        }
    }

    /**
     * 写入两级缓存。
     */
    public void put(String key, Object value, Duration ttl) {
        if (value == null) {
            return;
        }
        local.put(key, value);
        try {
            redis.opsForValue().set(key, objectMapper.writeValueAsString(value), ttl);
        } catch (Exception ex) {
            log.warn("写入多级缓存失败 key={}", key, ex);
        }
    }

    /**
     * 删除两级缓存。
     */
    public void evict(String key) {
        local.invalidate(key);
        try {
            redis.delete(key);
        } catch (Exception ex) {
            log.warn("删除多级缓存失败 key={}", key, ex);
        }
    }
}
