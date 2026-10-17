package com.laotang.core.ai.rag.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.laotang.core.ai.rag.entity.KnowledgeChunk;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 知识库分片 Mapper。
 */
@Mapper
public interface KnowledgeChunkMapper extends BaseMapper<KnowledgeChunk> {

    @Select("""
            SELECT id, doc_id, scope, title, section, content, embedding,
                   dimension, content_hash, enabled
            FROM tb_knowledge_chunk
            WHERE enabled = 1
              AND scope IN (#{scope}, 'ALL')
            ORDER BY id
            LIMIT #{limit}
            """)
    List<KnowledgeChunk> selectVectorCandidates(
            @Param("scope") String scope, @Param("limit") int limit);

    /**
     * ngram 全文索引召回。中文查询依赖 MySQL 自带的 ngram 解析器。
     */
    @Select("""
            SELECT id, doc_id, scope, title, section, content, embedding,
                   dimension, content_hash, enabled,
                   MATCH(title, content) AGAINST (#{query} IN NATURAL LANGUAGE MODE) AS score
            FROM tb_knowledge_chunk
            WHERE enabled = 1
              AND scope IN (#{scope}, 'ALL')
              AND MATCH(title, content) AGAINST (#{query} IN NATURAL LANGUAGE MODE)
            ORDER BY score DESC
            LIMIT #{limit}
            """)
    List<KnowledgeChunk> selectByKeyword(
            @Param("scope") String scope,
            @Param("query") String query,
            @Param("limit") int limit);

    /**
     * 全文索引不可用时的兜底召回。
     */
    @Select("""
            SELECT id, doc_id, scope, title, section, content, embedding,
                   dimension, content_hash, enabled
            FROM tb_knowledge_chunk
            WHERE enabled = 1
              AND scope IN (#{scope}, 'ALL')
              AND (title LIKE CONCAT('%', #{query}, '%')
                   OR content LIKE CONCAT('%', #{query}, '%'))
            ORDER BY id DESC
            LIMIT #{limit}
            """)
    List<KnowledgeChunk> selectByLike(
            @Param("scope") String scope,
            @Param("query") String query,
            @Param("limit") int limit);

    @Select("""
            SELECT id, doc_id, scope, title, section, content, embedding,
                   dimension, content_hash, enabled
            FROM tb_knowledge_chunk
            WHERE doc_id = #{docId}
            """)
    List<KnowledgeChunk> selectByDocId(@Param("docId") String docId);
}
