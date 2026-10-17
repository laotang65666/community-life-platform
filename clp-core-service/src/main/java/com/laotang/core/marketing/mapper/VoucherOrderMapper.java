package com.laotang.core.marketing.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.laotang.core.marketing.entity.VoucherOrder;
import org.apache.ibatis.annotations.Mapper;

/**
 * 秒杀订单数据访问。
 */
@Mapper
public interface VoucherOrderMapper extends BaseMapper<VoucherOrder> {
}
