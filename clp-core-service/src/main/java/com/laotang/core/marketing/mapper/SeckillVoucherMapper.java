package com.laotang.core.marketing.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.laotang.core.marketing.entity.SeckillVoucher;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 秒杀库存数据访问。
 */
@Mapper
public interface SeckillVoucherMapper extends BaseMapper<SeckillVoucher> {

    /**
     * DB 侧扣减,仅用于审计和对账;真正拦截并发的是 Redis。
     */
    @Update("""
            UPDATE tb_seckill_voucher
            SET stock = stock - 1, version = version + 1
            WHERE voucher_id = #{voucherId} AND stock > 0
            """)
    int deductStock(@Param("voucherId") Long voucherId);
}
