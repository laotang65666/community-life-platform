package com.laotang.core.shop.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 店铺分类,对应 tb_shop_type。
 */
@Data
@TableName("tb_shop_type")
public class ShopType {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String name;

    private String icon;

    private Integer sort;

    private LocalDateTime createdAt;
}
