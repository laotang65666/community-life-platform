package com.laotang.core.ai.rag;

import com.laotang.core.ai.rag.config.RagProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Markdown 知识切分。
 *
 * <p>按标题划分章节,章节内按段落累积;超过长度上限时按句号切分。
 * 每个分片记录所属章节,既用于展示,也参与哈希与向量化。</p>
 */
@Component
@RequiredArgsConstructor
public class KnowledgeChunker {

    private static final Pattern HEADING = Pattern.compile("^#{1,6}\\s+");
    private static final Pattern SENTENCE = Pattern.compile("(?<=[。！？!?])");

    private final RagProperties properties;

    public List<ChunkDraft> split(String title, String content) {
        List<ChunkDraft> drafts = new ArrayList<>();
        if (content == null || content.isBlank()) {
            return drafts;
        }

        String section = "";
        StringBuilder buffer = new StringBuilder();

        for (String rawLine : content.replace("\r\n", "\n").split("\n")) {
            String line = rawLine.strip();
            if (line.isEmpty()) {
                continue;
            }
            if (HEADING.matcher(line).find()) {
                flush(title, section, buffer, drafts);
                section = HEADING.matcher(line).replaceFirst("").strip();
                continue;
            }
            appendText(title, section, line, buffer, drafts);
        }
        flush(title, section, buffer, drafts);
        return drafts;
    }

    private void appendText(
            String title, String section, String text,
            StringBuilder buffer, List<ChunkDraft> drafts) {

        int max = properties.getMaxChunkChars();
        if (buffer.length() > 0 && buffer.length() + text.length() + 1 > max) {
            flush(title, section, buffer, drafts);
        }

        if (text.length() > max) {
            for (String sentence : SENTENCE.split(text)) {
                if (sentence.isBlank()) {
                    continue;
                }
                if (buffer.length() > 0 && buffer.length() + sentence.length() > max) {
                    flush(title, section, buffer, drafts);
                }
                buffer.append(sentence);
            }
            return;
        }
        buffer.append(text).append('\n');
    }

    private void flush(
            String title, String section,
            StringBuilder buffer, List<ChunkDraft> drafts) {

        String chunkContent = buffer.toString().strip();
        buffer.setLength(0);
        if (chunkContent.isEmpty()) {
            return;
        }
        String hash = TextHasher.sha256(title + "\n" + section + "\n" + chunkContent);
        drafts.add(new ChunkDraft(section, chunkContent, hash));
    }

    /**
     * 切分结果。
     */
    public record ChunkDraft(String section, String content, String hash) {
    }
}
