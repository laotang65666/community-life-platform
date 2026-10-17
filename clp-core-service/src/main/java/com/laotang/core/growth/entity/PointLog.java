package com.laotang.core.growth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 积分流水,对应 tb_point_log。只增不改。
 */
@Data
@TableName("tb_point_log")
public class PointLog {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long userId;

    /**
     * 变动值:正数增加,负数扣减。
     */
    private Integer changeAmount;

    /**
     * 变动后的余额。
     */
    private Integer balance;

    /**
     * SIGN_IN / SIGN_IN_STREAK / ORDER / ADMIN / EXCHANGE
     */
    private String source;

    private String bizId;

    private String remark;

    private LocalDateTime createdAt;
}
