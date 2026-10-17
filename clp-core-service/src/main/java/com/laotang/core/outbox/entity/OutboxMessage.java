package com.laotang.core.outbox.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 本地消息表,对应 tb_outbox。
 */
@Data
@TableName("tb_outbox")
public class OutboxMessage {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String messageId;

    private String topic;

    private String bizType;

    private String bizId;

    private String payload;

    /**
     * PENDING / SENT / ACKED / FAILED
     */
    private String status;

    private Integer retryCount;

    private LocalDateTime nextRetryAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
