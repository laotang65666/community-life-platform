package com.laotang.core.ai.rag;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.List;

/**
 * 一次检索的最终证据集合。
 */
public record KnowledgeContext(List<KnowledgeSnippet> snippets) {

    public KnowledgeContext {
        snippets = snippets == null ? List.of() : List.copyOf(snippets);
    }

    public static KnowledgeContext empty() {
        return new KnowledgeContext(List.of());
    }

    @JsonIgnore
    public boolean isEmpty() {
        return snippets.isEmpty();
    }

    /**
     * 转成提示词里的"参考资料"区块。编号供模型在回答中引用。
     */
    public String toPromptBlock() {
        StringBuilder builder = new StringBuilder("## 参考资料\n");
        for (int i = 0; i < snippets.size(); i++) {
            KnowledgeSnippet snippet = snippets.get(i);
            builder.append('[').append(i + 1).append("] ").append(snippet.title());
            if (snippet.section() != null && !snippet.section().isBlank()) {
                builder.append(" / ").append(snippet.section());
            }
            builder.append('\n').append(snippet.content().strip()).append("\n\n");
        }
        builder.append("""
                以上是平台内部知识。回答时优先使用参考资料;资料没有的信息不要编造。
                引用资料时在句末标注编号,例如 [1]。
                """);
        return builder.toString();
    }
}
