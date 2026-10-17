package com.laotang.core.order.support;

import com.laotang.core.order.service.IOrderService;
import com.laotang.framework.lock.delay.RedissonDelayQueue;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 订阅订单超时延时队列。
 */
@Component
@RequiredArgsConstructor
public class OrderTimeoutSubscriber {

    private static final String ORDER_CLOSE_QUEUE = "clp:delay:order-close";

    private final RedissonDelayQueue delayQueue;
    private final IOrderService orderService;

    @PostConstruct
    public void subscribe() {
        delayQueue.subscribe(ORDER_CLOSE_QUEUE,
                orderId -> orderService.closeIfTimeout(Long.valueOf(orderId)));
    }
}
