package com.laotang.core.ai.rag;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class VectorMathTest {

    @Test
    @DisplayName("同向向量余弦为 1")
    void cosine_sameDirection() {
        assertThat(VectorMath.cosine(
                new float[]{1, 0, 0}, new float[]{2, 0, 0})).isCloseTo(1.0, within(0.0001));
    }

    @Test
    @DisplayName("正交向量余弦为 0")
    void cosine_orthogonal() {
        assertThat(VectorMath.cosine(
                new float[]{1, 0}, new float[]{0, 1})).isCloseTo(0.0, within(0.0001));
    }

    @Test
    @DisplayName("维度不一致或空向量返回 0")
    void cosine_invalid() {
        assertThat(VectorMath.cosine(new float[]{1, 0}, new float[]{1})).isZero();
        assertThat(VectorMath.cosine(null, new float[]{1})).isZero();
        assertThat(VectorMath.cosine(new float[0], new float[0])).isZero();
    }

    @Test
    @DisplayName("文本哈希稳定且长度固定")
    void sha256() {
        String a = TextHasher.sha256("hello");
        String b = TextHasher.sha256("hello");

        assertThat(a).isEqualTo(b).hasSize(64);
        assertThat(TextHasher.sha256("hello!")).isNotEqualTo(a);
    }

    @Test
    @DisplayName("查询归一化忽略大小写与多余空白")
    void normalize() {
        assertThat(TextHasher.normalize("  How   To  Cancel ")).isEqualTo("how to cancel");
    }

    private static org.assertj.core.data.Offset<Double> within(double value) {
        return org.assertj.core.data.Offset.offset(value);
    }
}
