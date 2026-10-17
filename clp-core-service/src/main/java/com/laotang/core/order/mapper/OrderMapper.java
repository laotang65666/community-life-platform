package com.laotang.core.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.laotang.core.order.entity.Order;
import org.apache.ibatis.annotations.Mapper;

/**
 * 订单数据访问。
 */
@Mapper
public interface OrderMapper extends BaseMapper<Order> {
}
