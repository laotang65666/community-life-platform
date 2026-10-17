package com.laotang.core.marketing.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.laotang.core.marketing.entity.VoucherOrder;
import com.laotang.core.marketing.kafka.SeckillOrderMessage;
import com.laotang.core.marketing.mapper.SeckillVoucherMapper;
import com.laotang.core.marketing.mapper.VoucherOrderMapper;
import com.laotang.core.outbox.service.OutboxService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 秒杀消息的消费逻辑。
 *
 * <p>幂等:order_no 唯一索引 + (voucher_id, user_id) 唯一索引。</p>
 */
@Service
@RequiredArgsConstructor
public class SeckillOrderConsumeService {

    private static final Logger log = LoggerFactory.getLogger(SeckillOrderConsumeService.class);

    private final VoucherOrderMapper voucherOrderMapper;
    private final SeckillVoucherMapper seckillVoucherMapper;
    private final OutboxService outboxService;

    @Transactional(rollbackFor = Exception.class)
    public void consume(SeckillOrderMessage message) {
        VoucherOrder existing = voucherOrderMapper.selectOne(
                new LambdaQueryWrapper<VoucherOrder>()
                        .eq(VoucherOrder::getOrderNo, message.orderNo()));

        if (existing == null) {
            VoucherOrder order = new VoucherOrder();
            order.setOrderNo(message.orderNo());
            order.setUserId(message.userId());
            order.setVoucherId(message.voucherId());
            order.setShopId(message.shopId());
            order.setAmount(message.amount());
            order.setStatus("CREATED");
            voucherOrderMapper.insert(order);

            // DB 侧库存扣减仅用于审计;真正拦截超卖的是 Redis
            int rows = seckillVoucherMapper.deductStock(message.voucherId());
            if (rows == 0) {
                log.warn("DB 秒杀库存扣减失败,请对账 voucherId={}", message.voucherId());
            }
        }

        outboxService.markAcked(message.messageId());
    }
}
