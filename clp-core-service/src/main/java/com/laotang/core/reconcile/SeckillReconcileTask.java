package com.laotang.core.reconcile;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.laotang.common.constant.RedisKey;
import com.laotang.core.marketing.entity.SeckillVoucher;
import com.laotang.core.marketing.entity.Voucher;
import com.laotang.core.marketing.entity.VoucherOrder;
import com.laotang.core.marketing.mapper.SeckillVoucherMapper;
import com.laotang.core.marketing.mapper.VoucherMapper;
import com.laotang.core.marketing.mapper.VoucherOrderMapper;
import com.laotang.core.reconcile.entity.ReconcileLog;
import com.laotang.core.reconcile.mapper.ReconcileLogMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 秒杀库存对账。
 *
 * <p>期望库存 = 初始库存 - 已创建订单数;与 Redis 当前库存比对,
 * 不一致就写 tb_reconcile_log,供人工或补偿任务处理。</p>
 */
@Component
@RequiredArgsConstructor
public class SeckillReconcileTask {

    private static final Logger log = LoggerFactory.getLogger(SeckillReconcileTask.class);
    private static final String BIZ_TYPE = "SECKILL_STOCK";

    private final VoucherMapper voucherMapper;
    private final SeckillVoucherMapper seckillVoucherMapper;
    private final VoucherOrderMapper voucherOrderMapper;
    private final ReconcileLogMapper reconcileLogMapper;
    private final StringRedisTemplate redis;

    @Scheduled(fixedDelay = 60_000L)
    public void reconcile() {
        List<Voucher> vouchers = voucherMapper.selectList(new LambdaQueryWrapper<Voucher>()
                .eq(Voucher::getType, "SECKILL")
                .eq(Voucher::getStatus, "PUBLISHED"));

        for (Voucher voucher : vouchers) {
            SeckillVoucher seckill = seckillVoucherMapper.selectOne(
                    new LambdaQueryWrapper<SeckillVoucher>()
                            .eq(SeckillVoucher::getVoucherId, voucher.getId()));
            if (seckill == null) {
                continue;
            }

            String redisValue = redis.opsForValue().get(RedisKey.seckillStock(voucher.getId()));
            long redisStock = redisValue == null ? -1L : Long.parseLong(redisValue);

            Long orderCount = voucherOrderMapper.selectCount(
                    new LambdaQueryWrapper<VoucherOrder>()
                            .eq(VoucherOrder::getVoucherId, voucher.getId())
                            .ne(VoucherOrder::getStatus, "CANCELLED"));
            long expected = seckill.getInitialStock() - (orderCount == null ? 0L : orderCount);

            if (redisStock != expected) {
                log.warn("秒杀库存不一致 voucherId={} redis={} expected={}",
                        voucher.getId(), redisStock, expected);
                upsertLog(voucher.getId(), redisStock, expected);
            }
        }
    }

    private void upsertLog(Long voucherId, long redisStock, long expected) {
        String bizId = String.valueOf(voucherId);
        ReconcileLog existing = reconcileLogMapper.selectOne(
                new LambdaQueryWrapper<ReconcileLog>()
                        .eq(ReconcileLog::getBizType, BIZ_TYPE)
                        .eq(ReconcileLog::getBizId, bizId));

        if (existing == null) {
            ReconcileLog record = new ReconcileLog();
            record.setBizType(BIZ_TYPE);
            record.setBizId(bizId);
            record.setRedisValue(String.valueOf(redisStock));
            record.setDbValue(String.valueOf(expected));
            record.setDiff(redisStock - expected);
            record.setStatus("PENDING");
            reconcileLogMapper.insert(record);
        } else {
            reconcileLogMapper.update(null, new LambdaUpdateWrapper<ReconcileLog>()
                    .eq(ReconcileLog::getId, existing.getId())
                    .set(ReconcileLog::getRedisValue, String.valueOf(redisStock))
                    .set(ReconcileLog::getDbValue, String.valueOf(expected))
                    .set(ReconcileLog::getDiff, redisStock - expected)
                    .set(ReconcileLog::getStatus, "PENDING"));
        }
    }
}
