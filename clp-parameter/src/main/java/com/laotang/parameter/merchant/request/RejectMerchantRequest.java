package com.laotang.parameter.merchant.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 管理员驳回入驻申请。
 */
public record RejectMerchantRequest(

        @NotBlank(message = "驳回原因不能为空")
        @Size(max = 500, message = "驳回原因不能超过 500 个字符")
        String reason
) {
}
