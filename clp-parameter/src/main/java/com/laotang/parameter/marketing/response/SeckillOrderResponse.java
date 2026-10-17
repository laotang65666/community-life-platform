package com.laotang.parameter.marketing.response;

/**
 * 秒杀下单受理结果。异步创建订单,客户端可轮询 my-order。
 */
public record SeckillOrderResponse(String orderNo, String status) {
}
