package com.laotang.parameter.merchant.response;

/**
 * 商户入驻信息。
 */
public record MerchantResponse(
        String id,
        String userId,
        String shopName,
        String licenseNo,
        String address,
        String contactName,
        String contactPhone,
        String auditStatus,
        String auditRemark,
        String createdAt
) {
}
