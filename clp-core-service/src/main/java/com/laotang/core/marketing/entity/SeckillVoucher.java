package com.laotang.core.marketing.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 秒杀库存,对应 tb_seckill_voucher。
 */
@Data
@TableName("tb_seckill_voucher")
public class SeckillVoucher {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long voucherId;

    /**
     * 剩余库存(DB 侧,最终以 Redis 为准,这里用于对账)。
     */
    private Integer stock;

    private Integer initialStock;

    private Integer perUserLimit;

    @Version
    private Integer version;

    private LocalDateTime beginTime;

    private LocalDateTime endTime;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
