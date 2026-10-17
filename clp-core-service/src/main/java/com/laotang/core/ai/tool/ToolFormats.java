package com.laotang.core.ai.tool;

import java.math.BigDecimal;

/**
 * 工具输出格式化。
 *
 * <p>金额统一从"分"转成"元"再交给模型,避免模型把 1250 说成 1250 元。</p>
 */
public final class ToolFormats {

    private static final int DEFAULT_LIMIT = 5;
    private static final int MAX_LIMIT = 10;

    private ToolFormats() {
    }

    public static String yuan(Long cents) {
        if (cents == null) {
            return "0.00";
        }
        return BigDecimal.valueOf(cents, 2).toPlainString();
    }

    public static int limit(Integer requested) {
        if (requested == null) {
            return DEFAULT_LIMIT;
        }
        return Math.max(1, Math.min(requested, MAX_LIMIT));
    }

    public static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public static String orderStatusText(String status) {
        return switch (status == null ? "" : status) {
            case "CREATED" -> "待支付";
            case "PAID" -> "已支付,待商家接单";
            case "ACCEPTED" -> "商家已接单";
            case "COMPLETED" -> "已完成";
            case "CANCELLED" -> "已取消";
            case "CLOSED" -> "已关闭(超时未支付)";
            case "REFUNDED" -> "已退款";
            default -> status;
        };
    }

    public static String shopStatusText(String status) {
        return switch (status == null ? "" : status) {
            case "OPEN" -> "营业中";
            case "CLOSED" -> "已打烊";
            default -> status;
        };
    }

    public static String productStatusText(String status) {
        return switch (status == null ? "" : status) {
            case "ON_SALE" -> "在售";
            case "OFF_SALE" -> "已下架";
            default -> status;
        };
    }

    public static String shopName(boolean found, String name) {
        return found && name != null ? name : "未知店铺";
    }
}
