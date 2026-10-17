package com.laotang.core.ai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI 会话,对应 tb_ai_conversation。
 */
@Data
@TableName("tb_ai_conversation")
public class AiConversation {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long userId;

    /**
     * USER / MERCHANT
     */
    private String scope;

    private String title;

    /**
     * 历史对话摘要,Phase D 引入。
     */
    private String summary;

    /**
     * 已摘要到的消息 ID,避免重复摘要。
     */
    private Long summaryMessageId;

    private LocalDateTime lastMessageAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
