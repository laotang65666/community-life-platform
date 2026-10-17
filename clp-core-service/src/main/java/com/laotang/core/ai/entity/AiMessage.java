package com.laotang.core.ai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI 消息,对应 tb_ai_message。
 */
@Data
@TableName("tb_ai_message")
public class AiMessage {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long conversationId;

    /**
     * USER / ASSISTANT / TOOL
     */
    private String role;

    private String content;

    /**
     * 工具调用记录,MySQL JSON 列。当前阶段不写入,直接按字符串存取。
     */
    private String toolCalls;

    private Integer tokensIn;

    private Integer tokensOut;

    private LocalDateTime createdAt;
}
