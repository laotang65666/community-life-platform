package com.laotang.core.order.service;

import com.laotang.common.result.PageResult;
import com.laotang.parameter.order.request.CreateOrderRequest;
import com.laotang.parameter.order.response.OrderBriefResponse;
import com.laotang.parameter.order.response.OrderResponse;

/**
 * 订单服务。
 */
public interface IOrderService {

    OrderResponse create(Long userId, String idempotencyKey, CreateOrderRequest request);

    OrderResponse getDetail(Long orderId, Long userId, Long merchantId);

    PageResult<OrderBriefResponse> listMyOrders(Long userId, String status, long page, long size);

    PageResult<OrderBriefResponse> listShopOrders(Long shopId, Long merchantId, String status,
                                                  long page, long size);

    void cancel(Long orderId, Long userId);

    void pay(Long orderId, Long userId);

    void accept(Long orderId, Long merchantId);

    void complete(Long orderId, Long merchantId);

    /**
     * 延时队列回调:只有仍是 CREATED 的订单才会被关闭。
     */
    void closeIfTimeout(Long orderId);

    /**
     * 定时兜底:扫描已超时的 CREATED 订单并关闭。
     */
    void closeTimeoutOrders();
}
