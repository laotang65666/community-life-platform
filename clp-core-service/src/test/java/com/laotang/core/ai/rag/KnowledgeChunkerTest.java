package com.laotang.core.ai.rag;

import com.laotang.core.ai.rag.config.RagProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class KnowledgeChunkerTest {

    private KnowledgeChunker chunker;

    @BeforeEach
    void setUp() {
        RagProperties properties = new RagProperties();
        properties.setMaxChunkChars(50);
        chunker = new KnowledgeChunker(properties);
    }

    @Test
    @DisplayName("按标题切分并记录章节")
    void split_byHeadings() {
        String content = """
                ## 下单
                用户只能对同一店铺的商品统一下单。
                ## 退款
                平台首期未开放线上退款流程。
                """;

        List<KnowledgeChunker.ChunkDraft> drafts = chunker.split("订单规则", content);

        assertThat(drafts).hasSize(2);
        assertThat(drafts.get(0).section()).isEqualTo("下单");
        assertThat(drafts.get(0).content()).contains("同一店铺");
        assertThat(drafts.get(1).section()).isEqualTo("退款");
        assertThat(drafts.get(1).content()).contains("未开放");
    }

    @Test
    @DisplayName("超长段落按句子切分成多个分片")
    void split_longParagraph() {
        String sentence = "这是一句用于测试切分的中文内容。";
        String content = "## 规则\n" + sentence.repeat(6);

        List<KnowledgeChunker.ChunkDraft> drafts = chunker.split("长文", content);

        assertThat(drafts.size()).isGreaterThan(1);
        assertThat(drafts).allSatisfy(draft ->
                assertThat(draft.content().length()).isLessThanOrEqualTo(50));
    }

    @Test
    @DisplayName("同一内容不同章节产生不同哈希")
    void hash_dependsOnSection() {
        String content = """
                ## 一
                相同内容。
                ## 二
                相同内容。
                """;

        List<KnowledgeChunker.ChunkDraft> drafts = chunker.split("标题", content);

        assertThat(drafts).hasSize(2);
        assertThat(drafts.get(0).hash()).isNotEqualTo(drafts.get(1).hash());
    }

    @Test
    @DisplayName("空内容返回空列表")
    void split_blank() {
        assertThat(chunker.split("标题", "  ")).isEmpty();
        assertThat(chunker.split("标题", null)).isEmpty();
    }
}
