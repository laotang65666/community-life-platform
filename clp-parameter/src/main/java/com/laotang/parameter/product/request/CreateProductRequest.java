package com.laotang.parameter.product.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 新增商品。价格单位:分。
 */
public record CreateProductRequest(

        @NotBlank(message = "商品名称不能为空")
        @Size(max = 128, message = "商品名称不能超过 128 个字符")
        String name,

        @NotNull(message = "价格不能为空")
        @Min(value = 1, message = "价格必须大于 0")
        Long price,

        @NotNull(message = "库存不能为空")
        @Min(value = 0, message = "库存不能为负数")
        Integer stock,

        @Size(max = 2000, message = "描述不能超过 2000 个字符")
        String description,

        @Size(max = 500, message = "图片 URL 不能超过 500 个字符")
        String image
) {
}
