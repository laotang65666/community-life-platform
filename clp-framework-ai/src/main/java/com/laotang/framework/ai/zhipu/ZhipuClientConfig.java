package com.laotang.framework.ai.zhipu;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * 智谱 HTTP 客户端。
 *
 * <p>不把 Authorization 设成默认请求头:Key 缺失时应用仍能启动,
 * 真正调用 embedding / rerank 时才抛出可读的业务异常。</p>
 */
@Configuration
public class ZhipuClientConfig {

    @Bean
    public RestClient zhipuRestClient(ZhipuProperties properties) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(properties.getConnectTimeoutMs());
        requestFactory.setReadTimeout(properties.getReadTimeoutMs());

        return RestClient.builder()
                .baseUrl(properties.getBaseUrl())
                .requestFactory(requestFactory)
                .build();
    }
}
