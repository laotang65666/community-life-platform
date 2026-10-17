package com.laotang.core.growth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 签到审计记录,对应 tb_sign。
 *
 * <p>Redis Bitmap 是每日签到的快速判断依据,本表用于审计、补签和连续签到统计。</p>
 */
@Data
@TableName("tb_sign")
public class SignRecord {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long userId;

    private LocalDate signDate;

    private Integer year;

    private Integer month;

    private LocalDateTime createdAt;
}
