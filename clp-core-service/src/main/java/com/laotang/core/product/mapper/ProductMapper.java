package com.laotang.core.product.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.laotang.core.product.entity.Product;
import org.apache.ibatis.annotations.Mapper;

/**
 * 商品数据访问。
 */
@Mapper
public interface ProductMapper extends BaseMapper<Product> {
}
