package com.laotang.core.marketing.converter;

import com.laotang.common.util.DateTimeUtils;
import com.laotang.core.marketing.entity.SeckillVoucher;
import com.laotang.core.marketing.entity.Voucher;
import com.laotang.core.marketing.entity.VoucherOrder;
import com.laotang.parameter.marketing.response.VoucherBriefResponse;
import com.laotang.parameter.marketing.response.VoucherOrderResponse;
import com.laotang.parameter.marketing.response.VoucherResponse;

/**
 * Voucher / SeckillVoucher / VoucherOrder 到 DTO 的转换。
 */
public final class VoucherConverter {

    private VoucherConverter() {
    }

    public static VoucherResponse toResponse(Voucher voucher, SeckillVoucher seckill) {
        return new VoucherResponse(
                String.valueOf(voucher.getId()),
                String.valueOf(voucher.getShopId()),
                voucher.getTitle(),
                voucher.getSubTitle(),
                voucher.getRules(),
                voucher.getPayValue(),
                voucher.getActualValue(),
                voucher.getType(),
                voucher.getStatus(),
                DateTimeUtils.toIso(voucher.getBeginTime()),
                DateTimeUtils.toIso(voucher.getEndTime()),
                seckill == null ? null : seckill.getStock(),
                seckill == null ? null : seckill.getPerUserLimit());
    }

    public static VoucherBriefResponse toBrief(Voucher voucher) {
        return new VoucherBriefResponse(
                String.valueOf(voucher.getId()),
                String.valueOf(voucher.getShopId()),
                voucher.getTitle(),
                voucher.getSubTitle(),
                voucher.getPayValue(),
                voucher.getActualValue(),
                voucher.getStatus(),
                DateTimeUtils.toIso(voucher.getBeginTime()),
                DateTimeUtils.toIso(voucher.getEndTime()));
    }

    public static VoucherOrderResponse toOrderResponse(VoucherOrder order) {
        return new VoucherOrderResponse(
                String.valueOf(order.getId()),
                order.getOrderNo(),
                String.valueOf(order.getVoucherId()),
                order.getAmount(),
                order.getStatus(),
                DateTimeUtils.toIso(order.getCreatedAt()));
    }
}
