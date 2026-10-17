package com.laotang.core.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.laotang.core.order.entity.OrderItem;
import org.apache.ibatis.annotations.Mapper;

/**
 * 订单明细数据访问。
 */
@Mapper
public interface OrderItemMapper extends BaseMapper<OrderItem> {
}
