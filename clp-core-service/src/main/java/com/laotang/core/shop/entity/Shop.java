package com.laotang.core.shop.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 店铺,对应 tb_shop。
 */
@Data
@TableName("tb_shop")
public class Shop {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long merchantId;

    private Long typeId;

    private String name;

    private String address;

    private BigDecimal lat;

    private BigDecimal lng;

    private String phone;

    private String openTime;

    private String closeTime;

    /**
     * OPEN / CLOSED
     */
    private String status;

    @TableLogic
    private Integer deleted;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
