package com.laotang.framework.redis.cache;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * 多级缓存参数。
 */
@Data
@ConfigurationProperties(prefix = "clp.cache")
public class CacheProperties {

    /**
     * 店铺详情在 Redis 的过期时间。
     */
    private Duration shopTtl = Duration.ofMinutes(30);

    /**
     * 商品详情在 Redis 的过期时间。
     *
     * <p>商品含库存,变化比店铺频繁,TTL 刻意更短。</p>
     */
    private Duration productTtl = Duration.ofSeconds(60);

    /**
     * 本地 Caffeine 的过期时间。多节点下 L1 无法跨节点失效,
     * 用较短的 TTL 限制脏读窗口。
     */
    private Duration localTtl = Duration.ofSeconds(60);

    /**
     * 空值缓存时间。用于防缓存穿透:不存在的 ID 也缓存一个空标记。
     */
    private Duration nullTtl = Duration.ofMinutes(2);

    /**
     * 本地缓存最大条目数。
     */
    private long maxSize = 10_000;
}
