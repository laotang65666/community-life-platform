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
 * 智谱 embedding-3 客户端。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ZhipuEmbeddingClient implements EmbeddingClient {

    private static final String EMBEDDINGS_PATH = "/api/paas/v4/embeddings";
    private static final int LOG_BODY_LIMIT = 500;

    private final RestClient zhipuRestClient;
    private final ZhipuProperties properties;

    @Override
    public float[] embed(String text) {
        return embedAll(List.of(text)).getFirst();
    }

    @Override
    public List<float[]> embedAll(List<String> texts) {
        if (texts == null || texts.isEmpty()) {
            return List.of();
        }
        requireApiKey();

        EmbeddingRequest request = new EmbeddingRequest(
                properties.getEmbeddingModel(),
                texts,
                properties.getEmbeddingDimensions());

        try {
            EmbeddingResponse response = zhipuRestClient.post()
                    .uri(EMBEDDINGS_PATH)
                    .header("Authorization", "Bearer " + properties.getApiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(EmbeddingResponse.class);

            if (response == null || response.data() == null) {
                throw new BusinessException(ErrorCode.AI_SERVICE_UNAVAILABLE, "向量服务返回空结果");
            }
            return response.data().stream()
                    .map(EmbeddingData::embedding)
                    .toList();
        } catch (BusinessException ex) {
            throw ex;
        } catch (RestClientResponseException ex) {
            log.error("智谱 embedding 调用失败, status={}, body={}",
                    ex.getStatusCode(), truncate(ex.getResponseBodyAsString()));
            throw new BusinessException(ErrorCode.AI_SERVICE_UNAVAILABLE, "向量服务暂不可用");
        } catch (RestClientException ex) {
            log.error("智谱 embedding 网络异常", ex);
            throw new BusinessException(ErrorCode.AI_SERVICE_UNAVAILABLE, "向量服务暂不可用");
        }
    }

    @Override
    public int dimensions() {
        return properties.getEmbeddingDimensions();
    }

    private void requireApiKey() {
        if (!StringUtils.hasText(properties.getApiKey())) {
            throw new BusinessException(ErrorCode.AI_SERVICE_UNAVAILABLE, "未配置智谱 API Key");
        }
    }

    private String truncate(String body) {
        if (body == null) {
            return "";
        }
        return body.length() <= LOG_BODY_LIMIT ? body : body.substring(0, LOG_BODY_LIMIT);
    }

    private record EmbeddingRequest(
            String model,
            List<String> input,
            Integer dimensions) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record EmbeddingResponse(
            List<EmbeddingData> data,
            Usage usage) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record EmbeddingData(
            float[] embedding,
            int index) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record Usage(
            @JsonProperty("prompt_tokens") Integer promptTokens,
            @JsonProperty("total_tokens") Integer totalTokens) {
    }
}
