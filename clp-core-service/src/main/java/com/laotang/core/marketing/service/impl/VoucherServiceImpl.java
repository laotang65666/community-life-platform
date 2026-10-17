package com.laotang.core.marketing.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.laotang.common.constant.RedisKey;
import com.laotang.common.exception.BusinessException;
import com.laotang.common.exception.ErrorCode;
import com.laotang.common.result.PageResult;
import com.laotang.core.marketing.converter.VoucherConverter;
import com.laotang.core.marketing.entity.SeckillVoucher;
import com.laotang.core.marketing.entity.Voucher;
import com.laotang.core.marketing.enums.VoucherStatus;
import com.laotang.core.marketing.enums.VoucherType;
import com.laotang.core.marketing.mapper.SeckillVoucherMapper;
import com.laotang.core.marketing.mapper.VoucherMapper;
import com.laotang.core.marketing.service.IVoucherService;
import com.laotang.core.marketing.support.SeckillVoucherCache;
import com.laotang.core.shop.entity.Shop;
import com.laotang.core.shop.service.IShopService;
import com.laotang.parameter.marketing.request.CreateVoucherRequest;
import com.laotang.parameter.marketing.response.VoucherBriefResponse;
import com.laotang.parameter.marketing.response.VoucherResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 优惠券服务实现。
 */
@Service
@RequiredArgsConstructor
public class VoucherServiceImpl implements IVoucherService {

    private final VoucherMapper voucherMapper;
    private final SeckillVoucherMapper seckillVoucherMapper;
    private final IShopService shopService;
    private final StringRedisTemplate redis;
    private final SeckillVoucherCache voucherCache;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public VoucherResponse create(Long merchantId, CreateVoucherRequest request) {
        if (!request.endTime().isAfter(request.beginTime())) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "结束时间必须晚于开始时间");
        }

        Shop shop = shopService.getById(request.shopId());
        if (shop == null) {
            throw new BusinessException(ErrorCode.SHOP_NOT_FOUND);
        }
        if (!shop.getMerchantId().equals(merchantId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        Voucher voucher = new Voucher();
        voucher.setShopId(request.shopId());
        voucher.setTitle(request.title());
        voucher.setSubTitle(request.subTitle() == null ? "" : request.subTitle());
        voucher.setRules(request.rules() == null ? "" : request.rules());
        voucher.setPayValue(request.payValue());
        voucher.setActualValue(request.actualValue());
        voucher.setType(VoucherType.SECKILL.name());
        voucher.setStatus(VoucherStatus.PUBLISHED.name());
        voucher.setBeginTime(request.beginTime());
        voucher.setEndTime(request.endTime());
        voucherMapper.insert(voucher);

        SeckillVoucher seckill = new SeckillVoucher();
        seckill.setVoucherId(voucher.getId());
        seckill.setStock(request.stock());
        seckill.setInitialStock(request.stock());
        seckill.setPerUserLimit(request.perUserLimit());
        seckill.setVersion(0);
        seckill.setBeginTime(request.beginTime());
        seckill.setEndTime(request.endTime());
        seckillVoucherMapper.insert(seckill);

        // 预热 Redis:券信息 + 库存;清空已购用户集合
        voucherCache.put(voucher);
        redis.opsForValue().set(RedisKey.seckillStock(voucher.getId()), String.valueOf(request.stock()));
        redis.delete(RedisKey.seckillUsers(voucher.getId()));

        return VoucherConverter.toResponse(voucher, seckill);
    }

    @Override
    public PageResult<VoucherBriefResponse> list(Long shopId, long page, long size) {
        LambdaQueryWrapper<Voucher> wrapper = new LambdaQueryWrapper<Voucher>()
                .eq(Voucher::getStatus, VoucherStatus.PUBLISHED.name())
                .orderByDesc(Voucher::getBeginTime);

        if (shopId != null) {
            wrapper.eq(Voucher::getShopId, shopId);
        }

        Page<Voucher> result = voucherMapper.selectPage(new Page<>(page, size), wrapper);
        List<VoucherBriefResponse> list = result.getRecords().stream()
                .map(VoucherConverter::toBrief)
                .toList();
        return PageResult.of(list, result.getCurrent(), result.getSize(), result.getTotal());
    }

    @Override
    public VoucherResponse getDetail(Long voucherId) {
        Voucher voucher = voucherMapper.selectById(voucherId);
        if (voucher == null) {
            throw new BusinessException(ErrorCode.VOUCHER_NOT_FOUND);
        }

        SeckillVoucher seckill = seckillVoucherMapper.selectOne(
                new LambdaQueryWrapper<SeckillVoucher>()
                        .eq(SeckillVoucher::getVoucherId, voucherId));

        return VoucherConverter.toResponse(voucher, seckill);
    }
}
