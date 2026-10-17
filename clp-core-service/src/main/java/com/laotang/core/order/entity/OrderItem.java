package com.laotang.core.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 订单明细,对应 tb_order_item。
 *
 * <p>保存商品快照,商品后续改名/调价不影响历史订单。</p>
 */
@Data
@TableName("tb_order_item")
public class OrderItem {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long orderId;

    private Long productId;

    private String productName;

    private String productImage;

    private Long price;

    private Integer quantity;

    private Long amount;

    private LocalDateTime createdAt;
}
