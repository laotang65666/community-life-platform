package com.laotang.parameter.marketing.request;

import jakarta.validation.constraints.NotBlank;

/**
 * 秒杀下单请求。
 */
public record SeckillOrderRequest(

        @NotBlank(message = "秒杀令牌不能为空")
        String token
) {
}
