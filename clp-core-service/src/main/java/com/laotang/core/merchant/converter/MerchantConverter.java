package com.laotang.core.merchant.converter;

import com.laotang.common.util.DateTimeUtils;
import com.laotang.core.merchant.entity.Merchant;
import com.laotang.parameter.merchant.response.MerchantResponse;

/**
 * Merchant 实体到 DTO 的转换。
 */
public final class MerchantConverter {

    private MerchantConverter() {
    }

    public static MerchantResponse toResponse(Merchant merchant) {
        return new MerchantResponse(
                String.valueOf(merchant.getId()),
                String.valueOf(merchant.getUserId()),
                merchant.getShopName(),
                merchant.getLicenseNo(),
                merchant.getAddress(),
                merchant.getContactName(),
                merchant.getContactPhone(),
                merchant.getAuditStatus(),
                merchant.getAuditRemark(),
                DateTimeUtils.toIso(merchant.getCreatedAt()));
    }
}
