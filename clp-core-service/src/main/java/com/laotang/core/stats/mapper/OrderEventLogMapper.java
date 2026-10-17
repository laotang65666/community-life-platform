package com.laotang.core.stats.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.laotang.core.stats.entity.OrderEventLog;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;

/**
 * 订单事件去重 Mapper。
 */
@Mapper
public interface OrderEventLogMapper extends BaseMapper<OrderEventLog> {

    /**
     * 依赖 uk_event_id 去重,返回 0 表示事件已处理过。
     */
    @Insert("""
            INSERT IGNORE INTO tb_order_event_log
              (id, event_id, event_type, order_id, shop_id, stat_date)
            VALUES
              (#{id}, #{eventId}, #{eventType}, #{orderId}, #{shopId}, #{statDate})
            """)
    int insertIgnore(
            @Param("id") Long id,
            @Param("eventId") String eventId,
            @Param("eventType") String eventType,
            @Param("orderId") Long orderId,
            @Param("shopId") Long shopId,
            @Param("statDate") LocalDate statDate);
}
