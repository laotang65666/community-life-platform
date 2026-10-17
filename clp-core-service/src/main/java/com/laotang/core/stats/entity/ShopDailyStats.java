package com.laotang.core.stats.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 店铺日统计,对应 tb_shop_daily_stats。
 *
 * <p>由订单事件异步累加,用于趋势报表;当天实时数据仍以 tb_order 为准。</p>
 */
@Data
@TableName("tb_shop_daily_stats")
public class ShopDailyStats {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long shopId;

    private LocalDate statDate;

    private Long orderCount;

    private Long paidCount;

    private Long completedCount;

    private Long cancelCount;

    private Long closeCount;

    /**
     * 成交额,单位分。
     */
    private Long gmv;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
