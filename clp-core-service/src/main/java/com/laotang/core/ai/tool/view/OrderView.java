package com.laotang.core.ai.tool.view;

/**
 * 订单列表项,只暴露模型回答问题需要的字段。
 *
 * @param amountYuan 元,两位小数
 */
public record OrderView(
        String orderNo,
        String shopName,
        String amountYuan,
        String status,
        String statusText,
        String createdAt
) {
}
