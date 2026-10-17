package com.laotang.parameter.product.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 修改商品。字段为 null 表示不修改。
 */
public record UpdateProductRequest(

        @Size(max = 128, message = "商品名称不能超过 128 个字符")
        String name,

        @Min(value = 1, message = "价格必须大于 0")
        Long price,

        @Min(value = 0, message = "库存不能为负数")
        Integer stock,

        @Size(max = 2000, message = "描述不能超过 2000 个字符")
        String description,

        @Size(max = 500, message = "图片 URL 不能超过 500 个字符")
        String image,

        @Pattern(regexp = "ON_SALE|OFF_SALE", message = "商品状态只能是 ON_SALE 或 OFF_SALE")
        String status
) {
}
