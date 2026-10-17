package com.laotang.framework.ai.zhipu;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.laotang.common.exception.BusinessException;
import com.laotang.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;

/**
 * 智谱 rerank 客户端。
 *
 * <p>用于 RAG 的精排阶段:先用向量召回一批候选,再用 rerank 把真正
 * 相关的文档顶到前面,减少把无关内容塞进上下文的概率。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ZhipuRerankClient implements RerankClient {

    private static final String RERANK_PATH = "/api/paas/v4/rerank";
    private static final int LOG_BODY_LIMIT = 500;

    private final RestClient zhipuRestClient;
    private final ZhipuProperties properties;

    @Override
    public List<RerankResult> rerank(String query, List<String> documents, int topN) {
        if (documents == null || documents.isEmpty()) {
            return List.of();
        }
        requireApiKey();

        RerankRequest request = new RerankRequest(
                properties.getRerankModel(),
                query,
                documents,
                Math.min(topN, documents.size()));

        try {
            RerankResponse response = zhipuRestClient.post()
                    .uri(RERANK_PATH)
                    .header("Authorization", "Bearer " + properties.getApiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(RerankResponse.class);

            if (response == null || response.results() == null) {
                throw new BusinessException(ErrorCode.AI_SERVICE_UNAVAILABLE, "重排服务返回空结果");
            }
            return response.results().stream()
                    .map(item -> toResult(item, documents))
                    .filter(java.util.Objects::nonNull)
                    .toList();
        } catch (BusinessException ex) {
            throw ex;
        } catch (RestClientResponseException ex) {
            log.error("智谱 rerank 调用失败, status={}, body={}",
                    ex.getStatusCode(), truncate(ex.getResponseBodyAsString()));
            throw new BusinessException(ErrorCode.AI_SERVICE_UNAVAILABLE, "重排服务暂不可用");
        } catch (RestClientException ex) {
            log.error("智谱 rerank 网络异常", ex);
            throw new BusinessException(ErrorCode.AI_SERVICE_UNAVAILABLE, "重排服务暂不可用");
        }
    }

    private void requireApiKey() {
        if (!StringUtils.hasText(properties.getApiKey())) {
            throw new BusinessException(ErrorCode.AI_SERVICE_UNAVAILABLE, "未配置智谱 API Key");
        }
    }

    /**
     * rerank(普通版)返回 index,rerank-pro 返回 document 原文。
     * 这里统一还原成入参下标;找不到对应文档的条目直接丢弃。
     */
    private RerankResult toResult(RerankItem item, List<String> documents) {
        Integer index = item.index();
        if (index == null && item.document() != null) {
            int found = documents.indexOf(item.document());
            index = found >= 0 ? found : null;
        }
        if (index == null || index < 0 || index >= documents.size()) {
            return null;
        }
        return new RerankResult(index, item.relevanceScore());
    }

    private String truncate(String body) {
        if (body == null) {
            return "";
        }
        return body.length() <= LOG_BODY_LIMIT ? body : body.substring(0, LOG_BODY_LIMIT);
    }

    private record RerankRequest(
            String model,
            String query,
            List<String> documents,
            @JsonProperty("top_n") int topN) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record RerankResponse(
            List<RerankItem> results) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record RerankItem(
            Integer index,
            String document,
            @JsonProperty("relevance_score") double relevanceScore) {
    }
}
