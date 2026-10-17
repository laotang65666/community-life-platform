package com.laotang.core.ai.tool;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ToolFormatsTest {

    @Test
    @DisplayName("金额按分转元,保留两位小数")
    void yuan() {
        assertThat(ToolFormats.yuan(1250L)).isEqualTo("12.50");
        assertThat(ToolFormats.yuan(5L)).isEqualTo("0.05");
        assertThat(ToolFormats.yuan(0L)).isEqualTo("0.00");
        assertThat(ToolFormats.yuan(null)).isEqualTo("0.00");
    }

    @Test
    @DisplayName("返回条数限制在 1-10,默认 5")
    void limit() {
        assertThat(ToolFormats.limit(null)).isEqualTo(5);
        assertThat(ToolFormats.limit(0)).isEqualTo(1);
        assertThat(ToolFormats.limit(8)).isEqualTo(8);
        assertThat(ToolFormats.limit(99)).isEqualTo(10);
    }

    @Test
    @DisplayName("状态码转中文")
    void statusText() {
        assertThat(ToolFormats.orderStatusText("CREATED")).isEqualTo("待支付");
        assertThat(ToolFormats.orderStatusText("CLOSED")).contains("超时");
        assertThat(ToolFormats.shopStatusText("OPEN")).isEqualTo("营业中");
        assertThat(ToolFormats.productStatusText("OFF_SALE")).isEqualTo("已下架");
    }

    @Test
    @DisplayName("空白字符串归一为 null")
    void trimToNull() {
        assertThat(ToolFormats.trimToNull("  ")).isNull();
        assertThat(ToolFormats.trimToNull(" PAID ")).isEqualTo("PAID");
    }
}
