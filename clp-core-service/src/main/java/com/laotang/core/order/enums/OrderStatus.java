package com.laotang.core.order.enums;

/**
 * 订单状态机。
 *
 * <pre>
 * CREATED ──支付──> PAID ──接单──> ACCEPTED ──完成──> COMPLETED
 *    │                │
 *    ├──取消──> CANCELLED
 *    └──超时──> CLOSED
 * </pre>
 */
public enum OrderStatus {

    CREATED,
    PAID,
    ACCEPTED,
    COMPLETED,
    CANCELLED,
    CLOSED,
    REFUNDED
}
