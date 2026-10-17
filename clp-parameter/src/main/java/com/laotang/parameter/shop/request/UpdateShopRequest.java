package com.laotang.parameter.shop.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 修改店铺信息。字段为 null 表示不修改。
 */
public record UpdateShopRequest(

        @Size(max = 128, message = "店铺名称不能超过 128 个字符")
        String name,

        @Size(max = 255, message = "地址不能超过 255 个字符")
        String address,

        @Size(max = 20, message = "电话不能超过 20 个字符")
        String phone,

        @Size(max = 20, message = "营业开始时间格式不正确")
        String openTime,

        @Size(max = 20, message = "营业结束时间格式不正确")
        String closeTime,

        @Pattern(regexp = "OPEN|CLOSED", message = "店铺状态只能是 OPEN 或 CLOSED")
        String status
) {
}
