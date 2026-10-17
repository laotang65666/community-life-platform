package com.laotang.core.merchant.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.laotang.common.exception.BusinessException;
import com.laotang.common.exception.ErrorCode;
import com.laotang.common.result.PageResult;
import com.laotang.core.merchant.converter.MerchantConverter;
import com.laotang.core.merchant.entity.Merchant;
import com.laotang.core.merchant.mapper.MerchantMapper;
import com.laotang.core.merchant.service.IMerchantService;
import com.laotang.core.shop.service.IShopService;
import com.laotang.core.user.service.IUserService;
import com.laotang.parameter.merchant.request.ApplyMerchantRequest;
import com.laotang.parameter.merchant.request.UpdateMerchantRequest;
import com.laotang.parameter.merchant.response.MerchantResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 商户入驻服务实现。
 */
@Service
@RequiredArgsConstructor
public class MerchantServiceImpl implements IMerchantService {

    private static final String PENDING = "PENDING";
    private static final String APPROVED = "APPROVED";
    private static final String REJECTED = "REJECTED";
    private static final String ROLE_MERCHANT = "MERCHANT";

    private final MerchantMapper merchantMapper;
    private final IUserService userService;
    private final IShopService shopService;

    @Override
    public MerchantResponse apply(Long userId, ApplyMerchantRequest request) {
        Merchant existing = findLatestByUserId(userId);

        if (existing != null) {
            if (PENDING.equals(existing.getAuditStatus())) {
                throw new BusinessException(ErrorCode.MERCHANT_AUDITING);
            }
            if (APPROVED.equals(existing.getAuditStatus())) {
                throw new BusinessException(ErrorCode.MERCHANT_ALREADY_APPROVED);
            }
            // REJECTED:复用原记录重新提交,避免营业执照唯一索引冲突
            updateForReapply(existing.getId(), request);
            return MerchantConverter.toResponse(merchantMapper.selectById(existing.getId()));
        }

        Merchant merchant = new Merchant();
        merchant.setUserId(userId);
        merchant.setShopName(request.shopName());
        merchant.setLicenseNo(request.licenseNo());
        merchant.setAddress(request.address());
        merchant.setContactName(request.contactName());
        merchant.setContactPhone(request.contactPhone());
        merchant.setAuditStatus(PENDING);
        merchant.setAuditRemark("");

        try {
            merchantMapper.insert(merchant);
        } catch (DuplicateKeyException e) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "营业执照号已被使用");
        }
        return MerchantConverter.toResponse(merchant);
    }

    @Override
    public MerchantResponse getMyMerchant(Long userId) {
        Merchant merchant = findLatestByUserId(userId);
        if (merchant == null) {
            throw new BusinessException(ErrorCode.MERCHANT_NOT_FOUND);
        }
        return MerchantConverter.toResponse(merchant);
    }

    @Override
    public Merchant getById(Long merchantId) {
        return merchantMapper.selectById(merchantId);
    }

    @Override
    public void updateMyMerchant(Long userId, UpdateMerchantRequest request) {
        Merchant merchant = findLatestByUserId(userId);
        if (merchant == null) {
            throw new BusinessException(ErrorCode.MERCHANT_NOT_FOUND);
        }
        if (APPROVED.equals(merchant.getAuditStatus())) {
            throw new BusinessException(ErrorCode.MERCHANT_NOT_APPROVED, "审核已通过,请通过店铺接口修改");
        }

        LambdaUpdateWrapper<Merchant> wrapper = new LambdaUpdateWrapper<Merchant>()
                .eq(Merchant::getId, merchant.getId());

        boolean changed = false;
        if (request.shopName() != null) {
            wrapper.set(Merchant::getShopName, request.shopName());
            changed = true;
        }
        if (request.address() != null) {
            wrapper.set(Merchant::getAddress, request.address());
            changed = true;
        }
        if (request.contactName() != null) {
            wrapper.set(Merchant::getContactName, request.contactName());
            changed = true;
        }
        if (request.contactPhone() != null) {
            wrapper.set(Merchant::getContactPhone, request.contactPhone());
            changed = true;
        }

        if (changed) {
            merchantMapper.update(null, wrapper);
        }
    }

    @Override
    public PageResult<MerchantResponse> listApplications(String status, long page, long size) {
        LambdaQueryWrapper<Merchant> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(status)) {
            wrapper.eq(Merchant::getAuditStatus, status);
        }
        wrapper.orderByDesc(Merchant::getCreatedAt);

        Page<Merchant> result = merchantMapper.selectPage(new Page<>(page, size), wrapper);
        List<MerchantResponse> list = result.getRecords().stream()
                .map(MerchantConverter::toResponse)
                .toList();
        return PageResult.of(list, result.getCurrent(), result.getSize(), result.getTotal());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approve(Long merchantId, Long adminId, Long typeId) {
        Merchant merchant = requirePending(merchantId);

        if (userService.getById(merchant.getUserId()) == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "申请人不存在");
        }

        shopService.createForMerchant(merchant, typeId);
        userService.updateRole(merchant.getUserId(), ROLE_MERCHANT);

        merchantMapper.update(null, new LambdaUpdateWrapper<Merchant>()
                .eq(Merchant::getId, merchantId)
                .set(Merchant::getAuditStatus, APPROVED)
                .set(Merchant::getAuditRemark, "")
                .set(Merchant::getAuditedBy, adminId)
                .set(Merchant::getAuditedAt, LocalDateTime.now()));
    }

    @Override
    public void reject(Long merchantId, Long adminId, String reason) {
        requirePending(merchantId);

        merchantMapper.update(null, new LambdaUpdateWrapper<Merchant>()
                .eq(Merchant::getId, merchantId)
                .set(Merchant::getAuditStatus, REJECTED)
                .set(Merchant::getAuditRemark, reason)
                .set(Merchant::getAuditedBy, adminId)
                .set(Merchant::getAuditedAt, LocalDateTime.now()));
    }

    @Override
    public Merchant getApprovedByUserId(Long userId) {
        Merchant merchant = findLatestByUserId(userId);
        return merchant != null && APPROVED.equals(merchant.getAuditStatus()) ? merchant : null;
    }

    private Merchant requirePending(Long merchantId) {
        Merchant merchant = merchantMapper.selectById(merchantId);
        if (merchant == null) {
            throw new BusinessException(ErrorCode.MERCHANT_NOT_FOUND);
        }
        if (!PENDING.equals(merchant.getAuditStatus())) {
            throw new BusinessException(ErrorCode.MERCHANT_AUDITING, "该申请不在待审核状态");
        }
        return merchant;
    }

    private Merchant findLatestByUserId(Long userId) {
        return merchantMapper.selectOne(new LambdaQueryWrapper<Merchant>()
                .eq(Merchant::getUserId, userId)
                .orderByDesc(Merchant::getCreatedAt)
                .last("LIMIT 1"));
    }

    private void updateForReapply(Long merchantId, ApplyMerchantRequest request) {
        try {
            merchantMapper.update(null, new LambdaUpdateWrapper<Merchant>()
                    .eq(Merchant::getId, merchantId)
                    .set(Merchant::getShopName, request.shopName())
                    .set(Merchant::getLicenseNo, request.licenseNo())
                    .set(Merchant::getAddress, request.address())
                    .set(Merchant::getContactName, request.contactName())
                    .set(Merchant::getContactPhone, request.contactPhone())
                    .set(Merchant::getAuditStatus, PENDING)
                    .set(Merchant::getAuditRemark, "")
                    .set(Merchant::getAuditedBy, null)
                    .set(Merchant::getAuditedAt, null));
        } catch (DuplicateKeyException e) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "营业执照号已被使用");
        }
    }
}
