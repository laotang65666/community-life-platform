package com.laotang.core.reconcile.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 对账日志,对应 tb_reconcile_log。
 */
@Data
@TableName("tb_reconcile_log")
public class ReconcileLog {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String bizType;

    private String bizId;

    private String redisValue;

    private String dbValue;

    private Long diff;

    /**
     * PENDING / RESOLVED / IGNORED
     */
    private String status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
