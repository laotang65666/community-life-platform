package com.laotang.core.shop.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.laotang.core.shop.entity.Shop;
import org.apache.ibatis.annotations.Mapper;

/**
 * 店铺数据访问。
 */
@Mapper
public interface ShopMapper extends BaseMapper<Shop> {
}
