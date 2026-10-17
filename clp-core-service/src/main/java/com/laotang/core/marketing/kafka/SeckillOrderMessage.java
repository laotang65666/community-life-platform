package com.laotang.core.marketing.kafka;

/**
 * 秒杀订单消息体。
 *
 * @param messageId Outbox 消息 ID,消费端用于回写 ACKED
 * @param orderNo   预生成的订单号,客户端用它轮询结果
 */
public record SeckillOrderMessage(
        String messageId,
        String orderNo,
        Long userId,
        Long voucherId,
        Long shopId,
        Long amount,
        long createdAt
) {
}
