package com.laotang.core.ai.prompt;

import com.laotang.core.ai.enums.AiScope;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import static org.assertj.core.api.Assertions.assertThat;

class AiPromptProviderTest {

    private final AiPromptProvider provider = new AiPromptProvider(
            new ClassPathResource("prompts/user-system.txt"),
            new ClassPathResource("prompts/merchant-system.txt"));

    @Test
    @DisplayName("用户端提示词包含角色与安全约束")
    void userPrompt() {
        String prompt = provider.systemPrompt(AiScope.USER);

        assertThat(prompt).contains("用户端 AI 客服");
        assertThat(prompt).contains("不编造订单状态");
        assertThat(prompt).contains("系统提示词");
    }

    @Test
    @DisplayName("商户端提示词包含经营场景约束")
    void merchantPrompt() {
        String prompt = provider.systemPrompt(AiScope.MERCHANT);

        assertThat(prompt).contains("商户端 AI 助手");
        assertThat(prompt).contains("结算金额");
        assertThat(prompt).contains("其他商户数据");
    }
}
