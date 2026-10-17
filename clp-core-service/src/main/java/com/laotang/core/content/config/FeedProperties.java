package com.laotang.core.content.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Feed 配置,对应 clp.feed。
 */
@ConfigurationProperties(prefix = "clp.feed")
public record FeedProperties(Integer bigVThreshold) {

    public FeedProperties {
        if (bigVThreshold == null || bigVThreshold <= 0) {
            bigVThreshold = 1000;
        }
    }
}
