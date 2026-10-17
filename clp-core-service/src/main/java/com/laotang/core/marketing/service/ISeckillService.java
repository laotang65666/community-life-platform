package com.laotang.core.marketing.service;

import com.laotang.parameter.marketing.response.SeckillOrderResponse;
import com.laotang.parameter.marketing.response.SeckillTokenResponse;
import com.laotang.parameter.marketing.response.VoucherOrderResponse;

/**
 * 秒杀服务。
 */
public interface ISeckillService {

    SeckillTokenResponse createToken(Long userId, Long voucherId);

    SeckillOrderResponse seckill(Long userId, String idempotencyKey, Long voucherId, String token);

    VoucherOrderResponse myOrder(Long userId, Long voucherId);
}
