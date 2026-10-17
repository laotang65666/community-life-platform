package com.laotang.parameter.order.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 创建订单。请求头必须携带 Idempotency-Key。
 */
public record CreateOrderRequest(

        @NotNull(message = "店铺 ID 不能为空")
        Long shopId,

        @NotEmpty(message = "订单明细不能为空")
        @Valid
        List<OrderItemRequest> items,

        @Size(max = 255, message = "备注不能超过 255 个字符")
        String remark
) {
}
