package com.laotang.core.product.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.laotang.core.product.entity.Product;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 商品数据访问。
 */
@Mapper
public interface ProductMapper extends BaseMapper<Product> {

    /**
     * 条件扣库存。影响行数为 0 表示库存不足 / 已下架 / 商品不属于该店铺 / 已删除。
     */
    @Update("""
            UPDATE tb_product
            SET stock = stock - #{qty}, sold = sold + #{qty}, version = version + 1
            WHERE id = #{productId}
              AND shop_id = #{shopId}
              AND status = 'ON_SALE'
              AND stock >= #{qty}
              AND deleted = 0
            """)
    int deductStock(@Param("productId") Long productId,
                    @Param("shopId") Long shopId,
                    @Param("qty") int qty);

    /**
     * 回滚库存(取消 / 超时关闭)。
     */
    @Update("""
            UPDATE tb_product
            SET stock = stock + #{qty}, sold = sold - #{qty}, version = version + 1
            WHERE id = #{productId}
              AND sold >= #{qty}
            """)
    int restoreStock(@Param("productId") Long productId,
                     @Param("qty") int qty);
}
