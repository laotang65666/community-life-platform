package com.laotang.core.marketing.support;

import com.laotang.common.constant.RedisKey;
import com.laotang.common.exception.BusinessException;
import com.laotang.common.exception.ErrorCode;
import com.laotang.core.marketing.entity.Voucher;
import com.laotang.core.marketing.mapper.VoucherMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;

/**
 * 秒杀券信息缓存。
 *
 * <p>秒杀路径只从 Redis Hash 读取券的 shopId / payValue / 时间窗,
 * 避免每个请求都查一次数据库。</p>
 */
@Component
@RequiredArgsConstructor
public class SeckillVoucherCache {

    private static final Duration TTL = Duration.ofDays(30);
    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");

    private final StringRedisTemplate redis;
    private final VoucherMapper voucherMapper;

    public VoucherInfo get(Long voucherId) {
        String key = RedisKey.seckillVoucher(voucherId);
        Map<Object, Object> cache = redis.opsForHash().entries(key);

        if (!cache.isEmpty()) {
            return new VoucherInfo(
                    Long.valueOf(String.valueOf(cache.get("shopId"))),
                    Long.valueOf(String.valueOf(cache.get("payValue"))),
                    Long.parseLong(String.valueOf(cache.get("beginAt"))),
                    Long.parseLong(String.valueOf(cache.get("endAt"))));
        }

        // 缓存未命中:回源数据库并回填
        Voucher voucher = voucherMapper.selectById(voucherId);
        if (voucher == null) {
            throw new BusinessException(ErrorCode.VOUCHER_NOT_FOUND);
        }
        VoucherInfo info = new VoucherInfo(
                voucher.getShopId(),
                voucher.getPayValue(),
                toEpoch(voucher.getBeginTime()),
                toEpoch(voucher.getEndTime()));
        put(voucherId, info);
        return info;
    }

    public void put(Voucher voucher) {
        put(voucher.getId(), new VoucherInfo(
                voucher.getShopId(),
                voucher.getPayValue(),
                toEpoch(voucher.getBeginTime()),
                toEpoch(voucher.getEndTime())));
    }

    private void put(Long voucherId, VoucherInfo info) {
        String key = RedisKey.seckillVoucher(voucherId);
        redis.opsForHash().putAll(key, Map.of(
                "shopId", String.valueOf(info.shopId()),
                "payValue", String.valueOf(info.payValue()),
                "beginAt", String.valueOf(info.beginAt()),
                "endAt", String.valueOf(info.endAt())));
        redis.expire(key, TTL);
    }

    private static long toEpoch(LocalDateTime time) {
        return time.atZone(ZONE).toInstant().toEpochMilli();
    }

    public record VoucherInfo(Long shopId, Long payValue, long beginAt, long endAt) {
    }
}
