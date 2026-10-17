package com.laotang.core.order.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * 订单配置,对应 application.yml 的 clp.order。
 */
@ConfigurationProperties(prefix = "clp.order")
public record OrderProperties(Duration timeout) {

    public OrderProperties {
        if (timeout == null) {
            timeout = Duration.ofMinutes(30);
        }
    }
}
