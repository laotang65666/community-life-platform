package com.laotang.core.merchant.service;

import com.laotang.common.result.PageResult;
import com.laotang.core.merchant.entity.Merchant;
import com.laotang.parameter.merchant.request.ApplyMerchantRequest;
import com.laotang.parameter.merchant.request.UpdateMerchantRequest;
import com.laotang.parameter.merchant.response.MerchantResponse;

/**
 * 商户入驻服务。
 */
public interface IMerchantService {

    MerchantResponse apply(Long userId, ApplyMerchantRequest request);

    MerchantResponse getMyMerchant(Long userId);

    void updateMyMerchant(Long userId, UpdateMerchantRequest request);

    PageResult<MerchantResponse> listApplications(String status, long page, long size);

    void approve(Long merchantId, Long adminId, Long typeId);

    void reject(Long merchantId, Long adminId, String reason);

    /**
     * 当前用户已通过审核的商户记录;未通过返回 null。
     */
    Merchant getApprovedByUserId(Long userId);
}
