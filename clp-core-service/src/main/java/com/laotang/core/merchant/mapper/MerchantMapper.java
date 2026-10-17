package com.laotang.core.merchant.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.laotang.core.merchant.entity.Merchant;
import org.apache.ibatis.annotations.Mapper;

/**
 * 商户入驻数据访问。
 */
@Mapper
public interface MerchantMapper extends BaseMapper<Merchant> {
}
