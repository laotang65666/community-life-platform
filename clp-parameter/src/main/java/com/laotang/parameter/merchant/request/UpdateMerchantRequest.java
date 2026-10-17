package com.laotang.parameter.merchant.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 修改入驻信息。字段为 null 表示不修改。
 */
public record UpdateMerchantRequest(

        @Size(max = 128, message = "店铺名称不能超过 128 个字符")
        String shopName,

        @Size(max = 255, message = "经营地址不能超过 255 个字符")
        String address,

        @Size(max = 50, message = "联系人不能超过 50 个字符")
        String contactName,

        @Pattern(regexp = "^1[3-9]\\d{9}$", message = "联系电话格式不正确")
        String contactPhone
) {
}
