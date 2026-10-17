package com.laotang.core.notification.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 站内通知,对应 tb_notification。
 */
@Data
@TableName("tb_notification")
public class Notification {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long userId;

    /**
     * ORDER_CREATED / ORDER_ACCEPTED / ORDER_COMPLETED / SECKILL_RESULT /
     * MERCHANT_AUDIT / SYSTEM_NOTICE
     */
    private String type;

    private String title;

    private String content;

    private String bizId;

    /**
     * 0 未读 / 1 已读
     */
    private Integer isRead;

    private LocalDateTime readAt;

    private LocalDateTime createdAt;
}
