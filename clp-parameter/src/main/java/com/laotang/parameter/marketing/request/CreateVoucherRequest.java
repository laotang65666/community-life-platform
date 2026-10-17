package com.laotang.parameter.marketing.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/**
 * 商户创建秒杀优惠券。金额单位:分。
 */
public record CreateVoucherRequest(

        @NotNull(message = "店铺 ID 不能为空")
        Long shopId,

        @NotBlank(message = "标题不能为空")
        @Size(max = 128, message = "标题不能超过 128 个字符")
        String title,

        @Size(max = 255, message = "副标题不能超过 255 个字符")
        String subTitle,

        @Size(max = 2000, message = "使用规则不能超过 2000 个字符")
        String rules,

        @NotNull(message = "支付金额不能为空")
        @Min(value = 1, message = "支付金额必须大于 0")
        Long payValue,

        @NotNull(message = "抵扣金额不能为空")
        @Min(value = 1, message = "抵扣金额必须大于 0")
        Long actualValue,

        @NotNull(message = "库存不能为空")
        @Min(value = 1, message = "库存必须大于 0")
        Integer stock,

        @NotNull(message = "每人限购不能为空")
        @Min(value = 1, message = "每人限购至少为 1")
        Integer perUserLimit,

        @NotNull(message = "开始时间不能为空")
        LocalDateTime beginTime,

        @NotNull(message = "结束时间不能为空")
        LocalDateTime endTime
) {
}
