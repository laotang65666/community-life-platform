package com.laotang.core.stats.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 订单事件消费去重表,对应 tb_order_event_log。
 */
@Data
@TableName("tb_order_event_log")
public class OrderEventLog {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 事件 ID,取 Outbox 的 messageId。
     */
    private String eventId;

    private String eventType;

    private Long orderId;

    private Long shopId;

    private LocalDate statDate;

    private LocalDateTime createdAt;
}
