package com.laotang.core.order.support;

import com.laotang.core.order.service.IOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 订单超时兜底任务。
 *
 * <p>延时队列保证及时性;这个任务保证正确性——即使 Redis 丢了延时消息,
 * 超时订单也会在一分钟内被扫到并关闭。</p>
 */
@Component
@RequiredArgsConstructor
public class OrderTimeoutScheduler {

    private static final long SCAN_INTERVAL_MS = 60_000L;

    private final IOrderService orderService;

    @Scheduled(fixedDelay = SCAN_INTERVAL_MS)
    public void closeTimeoutOrders() {
        orderService.closeTimeoutOrders();
    }
}
