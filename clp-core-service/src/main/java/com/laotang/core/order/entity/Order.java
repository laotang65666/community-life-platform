package com.laotang.core.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 订单,对应 tb_order。
 */
@Data
@TableName("tb_order")
public class Order {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String orderNo;

    private Long userId;

    private Long shopId;

    /**
     * 订单总额,单位:分。
     */
    private Long totalAmount;

    /**
     * CREATED / PAID / ACCEPTED / COMPLETED / CANCELLED / CLOSED / REFUNDED
     */
    private String status;

    private String remark;

    private LocalDateTime payTime;

    private LocalDateTime acceptTime;

    private LocalDateTime completeTime;

    private LocalDateTime cancelTime;

    private String cancelReason;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
