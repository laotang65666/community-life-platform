package com.laotang.core.ai.rag;

import com.laotang.common.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

class KnowledgeScopeTest {

    @Test
    @DisplayName("scope 解析忽略大小写与空白")
    void from() {
        assertThat(KnowledgeScope.from("user")).isEqualTo(KnowledgeScope.USER);
        assertThat(KnowledgeScope.from(" MERCHANT ")).isEqualTo(KnowledgeScope.MERCHANT);
        assertThat(KnowledgeScope.from("all")).isEqualTo(KnowledgeScope.ALL);
    }

    @Test
    @DisplayName("未知 scope 抛参数错误")
    void from_unknown() {
        BusinessException ex = catchThrowableOfType(
                () -> KnowledgeScope.from("OTHER"), BusinessException.class);

        assertThat(ex).isNotNull();
        assertThat(ex.getMessage()).contains("OTHER");
    }
}
