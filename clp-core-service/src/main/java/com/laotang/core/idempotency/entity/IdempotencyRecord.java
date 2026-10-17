package com.laotang.core.idempotency.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 幂等记录,对应 tb_idempotency。
 */
@Data
@TableName("tb_idempotency")
public class IdempotencyRecord {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String idemKey;

    private Long userId;

    private String bizType;

    private String bizId;

    private String responseSnapshot;

    /**
     * PROCESSING / SUCCESS / FAILED
     */
    private String status;

    private LocalDateTime expireAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
