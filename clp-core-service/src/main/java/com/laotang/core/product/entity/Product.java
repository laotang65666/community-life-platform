package com.laotang.core.product.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 商品,对应 tb_product。
 */
@Data
@TableName("tb_product")
public class Product {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long shopId;

    private String name;

    /**
     * 单价,单位:分。
     */
    private Long price;

    private Integer stock;

    private Integer sold;

    private String description;

    private String image;

    /**
     * ON_SALE / OFF_SALE
     */
    private String status;

    @Version
    private Integer version;

    @TableLogic
    private Integer deleted;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
