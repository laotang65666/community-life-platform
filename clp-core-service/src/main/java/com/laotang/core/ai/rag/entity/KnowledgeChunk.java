package com.laotang.core.ai.rag.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 知识库分片,对应 tb_knowledge_chunk。
 */
@Data
@TableName("tb_knowledge_chunk")
public class KnowledgeChunk {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String docId;

    /**
     * USER / MERCHANT / ALL
     */
    private String scope;

    private String title;

    private String section;

    private String content;

    /**
     * MySQL JSON 列,内容是 float 数组。
     */
    private String embedding;

    private Integer dimension;

    private String contentHash;

    private Integer enabled;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    /**
     * 全文检索得分,仅关键词召回时由 SQL 填充,不落库。
     */
    @TableField(exist = false)
    private Double score;
}
