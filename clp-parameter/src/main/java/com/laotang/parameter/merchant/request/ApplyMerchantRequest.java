package com.laotang.parameter.merchant.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 商户入驻申请。
 */
public record ApplyMerchantRequest(

        @NotBlank(message = "店铺名称不能为空")
        @Size(max = 128, message = "店铺名称不能超过 128 个字符")
        String shopName,

        @NotBlank(message = "营业执照号不能为空")
        @Size(max = 64, message = "营业执照号不能超过 64 个字符")
        String licenseNo,

        @NotBlank(message = "经营地址不能为空")
        @Size(max = 255, message = "经营地址不能超过 255 个字符")
        String address,

        @NotBlank(message = "联系人不能为空")
        @Size(max = 50, message = "联系人不能超过 50 个字符")
        String contactName,

        @NotBlank(message = "联系电话不能为空")
        @Pattern(regexp = "^1[3-9]\\d{9}$", message = "联系电话格式不正确")
        String contactPhone
) {
}
