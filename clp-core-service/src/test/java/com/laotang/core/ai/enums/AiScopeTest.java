package com.laotang.core.ai.enums;

import com.laotang.common.enums.RoleEnum;
import com.laotang.common.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

class AiScopeTest {

    @Test
    @DisplayName("scope 解析忽略大小写")
    void from_ignoreCase() {
        assertThat(AiScope.from("user")).isEqualTo(AiScope.USER);
        assertThat(AiScope.from(" MERCHANT ")).isEqualTo(AiScope.MERCHANT);
    }

    @Test
    @DisplayName("未知 scope 抛参数错误")
    void from_unknown_throws() {
        BusinessException ex = catchThrowableOfType(
                () -> AiScope.from("ADMIN"), BusinessException.class);

        assertThat(ex).isNotNull();
        assertThat(ex.getMessage()).contains("ADMIN");
    }

    @Test
    @DisplayName("商户助手只对商户及以上角色开放")
    void availableTo_checksRole() {
        assertThat(AiScope.USER.availableTo(RoleEnum.USER)).isTrue();
        assertThat(AiScope.MERCHANT.availableTo(RoleEnum.USER)).isFalse();
        assertThat(AiScope.MERCHANT.availableTo(RoleEnum.MERCHANT)).isTrue();
        assertThat(AiScope.MERCHANT.availableTo(RoleEnum.ADMIN)).isTrue();
        assertThat(AiScope.USER.availableTo(null)).isFalse();
    }
}
