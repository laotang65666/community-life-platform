package com.laotang.core.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.laotang.core.order.entity.Order;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;

/**
 * 订单数据访问。
 */
@Mapper
public interface OrderMapper extends BaseMapper<Order> {

    @Select("""
            SELECT COUNT(*)
            FROM tb_order
            WHERE shop_id = #{shopId}
              AND created_at >= #{start}
            """)
    long countCreatedSince(@Param("shopId") Long shopId, @Param("start") LocalDateTime start);

    @Select("""
            SELECT COUNT(*)
            FROM tb_order
            WHERE shop_id = #{shopId}
              AND created_at >= #{start}
              AND status IN ('PAID', 'ACCEPTED', 'COMPLETED')
            """)
    long countPaidSince(@Param("shopId") Long shopId, @Param("start") LocalDateTime start);

    @Select("""
            SELECT COALESCE(SUM(total_amount), 0)
            FROM tb_order
            WHERE shop_id = #{shopId}
              AND created_at >= #{start}
              AND status IN ('PAID', 'ACCEPTED', 'COMPLETED')
            """)
    long sumPaidAmountSince(@Param("shopId") Long shopId, @Param("start") LocalDateTime start);

    @Select("""
            SELECT COUNT(*)
            FROM tb_order
            WHERE shop_id = #{shopId}
              AND status = 'PAID'
            """)
    long countPendingAccept(@Param("shopId") Long shopId);
}
