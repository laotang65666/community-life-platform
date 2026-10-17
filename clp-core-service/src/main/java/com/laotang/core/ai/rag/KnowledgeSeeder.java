package com.laotang.core.ai.rag;

import com.laotang.core.ai.rag.config.RagProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * 启动时导入 classpath:knowledge/*.md 种子知识。
 *
 * <p>文件命名约定:{@code user-xxx.md} 仅用户端可见,
 * {@code merchant-xxx.md} 仅商户端可见,其他为两端通用。</p>
 *
 * <p>导入失败只记录日志,不阻断应用启动;没有配置智谱 Key 时
 * AI 对话仍可用,只是没有 RAG 证据。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KnowledgeSeeder implements ApplicationRunner {

    private static final String SEED_PATTERN = "classpath*:knowledge/*.md";
    private static final String DOC_PREFIX = "seed:";

    private final KnowledgeIngestionService ingestionService;
    private final RagProperties properties;

    @Override
    public void run(ApplicationArguments args) {
        if (!properties.isSeedEnabled()) {
            return;
        }
        ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        try {
            Resource[] resources = resolver.getResources(SEED_PATTERN);
            Arrays.sort(resources, (a, b) ->
                    String.valueOf(a.getFilename()).compareTo(String.valueOf(b.getFilename())));
            for (Resource resource : resources) {
                ingest(resource);
            }
            log.info("知识库种子检查完成,共 {} 个文件", resources.length);
        } catch (Exception ex) {
            log.warn("知识库种子导入跳过: {}", ex.getMessage());
        }
    }

    private void ingest(Resource resource) throws Exception {
        String filename = resource.getFilename();
        if (filename == null || !filename.endsWith(".md")) {
            return;
        }
        String name = filename.substring(0, filename.length() - 3);
        String docId = DOC_PREFIX + name;

        String raw = resource.getContentAsString(StandardCharsets.UTF_8);
        String[] lines = raw.replace("\r\n", "\n").split("\n", -1);

        String title = name;
        StringBuilder body = new StringBuilder();
        boolean titleRemoved = false;
        for (String line : lines) {
            if (!titleRemoved && line.startsWith("# ")) {
                title = line.substring(2).strip();
                titleRemoved = true;
                continue;
            }
            body.append(line).append('\n');
        }

        KnowledgeScope scope = scopeOf(name);
        ingestionService.ingest(docId, scope, title, body.toString());
    }

    private KnowledgeScope scopeOf(String name) {
        if (name.startsWith("user-")) {
            return KnowledgeScope.USER;
        }
        if (name.startsWith("merchant-")) {
            return KnowledgeScope.MERCHANT;
        }
        return KnowledgeScope.ALL;
    }
}
