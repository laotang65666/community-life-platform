package com.laotang.core.stats.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.laotang.core.stats.entity.ShopDailyStats;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

/**
 * 店铺日统计 Mapper。
 */
@Mapper
public interface ShopDailyStatsMapper extends BaseMapper<ShopDailyStats> {

    /**
     * 幂等累加:同店同日只保留一行。
     */
    @Insert("""
            INSERT INTO tb_shop_daily_stats
              (id, shop_id, stat_date, order_count, paid_count, completed_count,
               cancel_count, close_count, gmv)
            VALUES
              (#{id}, #{shopId}, #{statDate}, #{orderCount}, #{paidCount}, #{completedCount},
               #{cancelCount}, #{closeCount}, #{gmv})
            ON DUPLICATE KEY UPDATE
              order_count = order_count + VALUES(order_count),
              paid_count = paid_count + VALUES(paid_count),
              completed_count = completed_count + VALUES(completed_count),
              cancel_count = cancel_count + VALUES(cancel_count),
              close_count = close_count + VALUES(close_count),
              gmv = gmv + VALUES(gmv)
            """)
    int upsert(
            @Param("id") Long id,
            @Param("shopId") Long shopId,
            @Param("statDate") LocalDate statDate,
            @Param("orderCount") long orderCount,
            @Param("paidCount") long paidCount,
            @Param("completedCount") long completedCount,
            @Param("cancelCount") long cancelCount,
            @Param("closeCount") long closeCount,
            @Param("gmv") long gmv);

    @Select("""
            SELECT id, shop_id, stat_date, order_count, paid_count, completed_count,
                   cancel_count, close_count, gmv
            FROM tb_shop_daily_stats
            WHERE shop_id = #{shopId}
              AND stat_date BETWEEN #{start} AND #{end}
            ORDER BY stat_date
            """)
    List<ShopDailyStats> selectShopTrend(
            @Param("shopId") Long shopId,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end);

    @Select("""
            SELECT stat_date,
                   SUM(order_count)     AS order_count,
                   SUM(paid_count)      AS paid_count,
                   SUM(completed_count) AS completed_count,
                   SUM(cancel_count)    AS cancel_count,
                   SUM(close_count)     AS close_count,
                   SUM(gmv)             AS gmv
            FROM tb_shop_daily_stats
            WHERE stat_date BETWEEN #{start} AND #{end}
            GROUP BY stat_date
            ORDER BY stat_date
            """)
    List<ShopDailyStats> sumPlatformTrend(
            @Param("start") LocalDate start,
            @Param("end") LocalDate end);
}
