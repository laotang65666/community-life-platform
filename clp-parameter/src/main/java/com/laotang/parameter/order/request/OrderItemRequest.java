package com.laotang.parameter.order.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * 下单明细项。
 */
public record OrderItemRequest(

        @NotNull(message = "商品 ID 不能为空")
        Long productId,

        @NotNull(message = "购买数量不能为空")
        @Min(value = 1, message = "购买数量至少为 1")
        Integer quantity
) {
}
