package com.laotang.core.marketing.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 秒杀订单,对应 tb_voucher_order。
 */
@Data
@TableName("tb_voucher_order")
public class VoucherOrder {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String orderNo;

    private Long userId;

    private Long voucherId;

    private Long shopId;

    private Long amount;

    /**
     * CREATED / PAID / CANCELLED
     */
    private String status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
