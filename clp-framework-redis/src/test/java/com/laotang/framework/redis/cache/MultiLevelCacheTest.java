package com.laotang.framework.redis.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MultiLevelCacheTest {

    @Mock
    private StringRedisTemplate redis;
    @Mock
    private ValueOperations<String, String> valueOps;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private MultiLevelCache cache;

    @BeforeEach
    void setUp() {
        CacheProperties properties = new CacheProperties();
        properties.setLocalTtl(Duration.ofMinutes(5));
        properties.setMaxSize(100);
        cache = new MultiLevelCache(redis, objectMapper, properties);
        lenient().when(redis.opsForValue()).thenReturn(valueOps);
    }

    @Test
    @DisplayName("写入后 L1 命中,不再访问 Redis")
    void put_thenGet_hitsLocal() {
        Sample sample = new Sample("a", 1);

        cache.put("k", sample, Duration.ofMinutes(10));
        Sample result = cache.get("k", Sample.class);

        assertThat(result).isEqualTo(sample);
        verify(valueOps, never()).get(anyString());
    }

    @Test
    @DisplayName("L1 未命中时读 Redis,并回填 L1")
    void get_redisHit_fillsLocal() throws Exception {
        Sample sample = new Sample("b", 2);
        when(valueOps.get("k")).thenReturn(objectMapper.writeValueAsString(sample));

        assertThat(cache.get("k", Sample.class)).isEqualTo(sample);
        assertThat(cache.get("k", Sample.class)).isEqualTo(sample);

        verify(valueOps, times(1)).get("k");
    }

    @Test
    @DisplayName("两级都未命中返回 null")
    void get_miss() {
        when(valueOps.get("k")).thenReturn(null);

        assertThat(cache.get("k", Sample.class)).isNull();
    }

    @Test
    @DisplayName("Redis 异常时按未命中处理,不抛给业务")
    void get_redisError_returnsNull() {
        when(valueOps.get("k")).thenThrow(new IllegalStateException("redis down"));

        assertThat(cache.get("k", Sample.class)).isNull();
    }

    @Test
    @DisplayName("删除同时清理 L1 与 Redis")
    void evict_removesBoth() {
        cache.put("k", new Sample("c", 3), Duration.ofMinutes(10));

        cache.evict("k");

        verify(redis).delete("k");
        assertThat(cache.get("k", Sample.class)).isNull();
    }

    @Test
    @DisplayName("写入 null 不产生缓存")
    void put_null_noop() {
        cache.put("k", null, Duration.ofMinutes(10));

        verify(valueOps, never()).set(anyString(), anyString(), eq(Duration.ofMinutes(10)));
        verify(redis, never()).delete(anyString());
    }

    private record Sample(String name, int value) {
    }
}
