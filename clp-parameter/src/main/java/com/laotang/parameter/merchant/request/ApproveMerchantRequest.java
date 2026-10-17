package com.laotang.parameter.merchant.request;

import jakarta.validation.constraints.NotNull;

/**
 * 管理员通过入驻申请,同时指定店铺分类。
 */
public record ApproveMerchantRequest(

        @NotNull(message = "店铺分类不能为空")
        Long typeId
) {
}
