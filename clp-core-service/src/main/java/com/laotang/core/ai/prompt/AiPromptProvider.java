package com.laotang.core.ai.prompt;

import com.laotang.core.ai.enums.AiScope;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

/**
 * 系统提示词提供者。
 *
 * <p>提示词放在 {@code resources/prompts/*.txt},修改后重启即可生效,
 * 不必改 Java 代码;也方便做提示词版本管理和 A/B 对比。</p>
 */
@Component
public class AiPromptProvider {

    private final String userSystemPrompt;
    private final String merchantSystemPrompt;

    public AiPromptProvider(
            @Value("classpath:prompts/user-system.txt") Resource userSystemPromptResource,
            @Value("classpath:prompts/merchant-system.txt") Resource merchantSystemPromptResource) {
        this.userSystemPrompt = read(userSystemPromptResource);
        this.merchantSystemPrompt = read(merchantSystemPromptResource);
    }

    public String systemPrompt(AiScope scope) {
        return switch (scope) {
            case USER -> userSystemPrompt;
            case MERCHANT -> merchantSystemPrompt;
        };
    }

    private String read(Resource resource) {
        try {
            return resource.getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new UncheckedIOException("读取系统提示词失败: " + resource.getDescription(), ex);
        }
    }
}
