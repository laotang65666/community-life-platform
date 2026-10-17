package com.laotang.core.marketing.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 优惠券,对应 tb_voucher。
 */
@Data
@TableName("tb_voucher")
public class Voucher {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long shopId;

    private String title;

    private String subTitle;

    private String rules;

    /**
     * 支付金额,单位:分。
     */
    private Long payValue;

    /**
     * 抵扣金额,单位:分。
     */
    private Long actualValue;

    /**
     * NORMAL / SECKILL
     */
    private String type;

    /**
     * DRAFT / PUBLISHED / ENDED
     */
    private String status;

    private LocalDateTime beginTime;

    private LocalDateTime endTime;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
