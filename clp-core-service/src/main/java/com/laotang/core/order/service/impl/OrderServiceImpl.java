package com.laotang.core.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.laotang.common.exception.BusinessException;
import com.laotang.common.exception.ErrorCode;
import com.laotang.common.result.PageResult;
import com.laotang.core.idempotency.entity.IdempotencyRecord;
import com.laotang.core.idempotency.service.IdempotencyService;
import com.laotang.core.merchant.entity.Merchant;
import com.laotang.core.merchant.service.IMerchantService;
import com.laotang.core.notification.service.INotificationService;
import com.laotang.core.order.config.OrderProperties;
import com.laotang.core.order.converter.OrderConverter;
import com.laotang.core.order.entity.Order;
import com.laotang.core.order.entity.OrderItem;
import com.laotang.core.order.enums.OrderStatus;
import com.laotang.core.order.mapper.OrderItemMapper;
import com.laotang.core.order.mapper.OrderMapper;
import com.laotang.core.order.service.IOrderService;
import com.laotang.core.order.service.OrderTxService;
import com.laotang.core.shop.entity.Shop;
import com.laotang.core.shop.service.IShopService;
import com.laotang.framework.lock.delay.RedissonDelayQueue;
import com.laotang.parameter.order.request.CreateOrderRequest;
import com.laotang.parameter.order.response.OrderBriefResponse;
import com.laotang.parameter.order.response.OrderResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 订单服务实现:幂等、延时关单、查询与编排。
 */
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements IOrderService {

    private static final String ORDER_CLOSE_QUEUE = "clp:delay:order-close";
    private static final String BIZ_TYPE = "ORDER";
    private static final int SCAN_BATCH_SIZE = 100;

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final OrderTxService orderTxService;
    private final IdempotencyService idempotencyService;
    private final RedissonDelayQueue delayQueue;
    private final OrderProperties orderProperties;
    private final IShopService shopService;
    private final IMerchantService merchantService;
    private final INotificationService notificationService;

    @Override
    public OrderResponse create(Long userId, String idempotencyKey, CreateOrderRequest request) {
        if (!StringUtils.hasText(idempotencyKey)) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "缺少 Idempotency-Key");
        }

        IdempotencyRecord acquired = idempotencyService.tryAcquire(userId, idempotencyKey, BIZ_TYPE);
        if (acquired == null) {
            IdempotencyRecord existing = idempotencyService.find(userId, idempotencyKey);
            if (existing == null) {
                throw new BusinessException(ErrorCode.REQUEST_PROCESSING);
            }
            if (IdempotencyService.SUCCESS.equals(existing.getStatus())) {
                return getDetail(Long.valueOf(existing.getBizId()), userId, null);
            }
            if (IdempotencyService.PROCESSING.equals(existing.getStatus())) {
                throw new BusinessException(ErrorCode.REQUEST_PROCESSING);
            }
            // FAILED:释放后重试一次
            idempotencyService.release(existing.getId());
            acquired = idempotencyService.tryAcquire(userId, idempotencyKey, BIZ_TYPE);
            if (acquired == null) {
                throw new BusinessException(ErrorCode.REQUEST_PROCESSING);
            }
        }

        try {
            OrderTxService.CreatedOrder created = orderTxService.create(userId, request);
            idempotencyService.markSuccess(acquired.getId(), created.order().getId());

            // 事务已提交,投递延时关单消息
            delayQueue.offer(
                    ORDER_CLOSE_QUEUE,
                    String.valueOf(created.order().getId()),
                    orderProperties.timeout().toMillis(),
                    TimeUnit.MILLISECONDS);

            notifyMerchant(created.order());

            return OrderConverter.toResponse(created.order(), created.items());
        } catch (RuntimeException e) {
            // 业务失败,释放幂等键,允许客户端重试
            idempotencyService.release(acquired.getId());
            throw e;
        }
    }

    @Override
    public OrderResponse getDetail(Long orderId, Long userId, Long merchantId) {
        Order order = requireOrder(orderId);

        boolean owner = order.getUserId().equals(userId);
        boolean merchant = merchantId != null && isShopOwner(order.getShopId(), merchantId);
        if (!owner && !merchant) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        return OrderConverter.toResponse(order, listItems(orderId));
    }

    @Override
    public PageResult<OrderBriefResponse> listMyOrders(Long userId, String status, long page, long size) {
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<Order>()
                .eq(Order::getUserId, userId)
                .orderByDesc(Order::getCreatedAt);
        if (StringUtils.hasText(status)) {
            wrapper.eq(Order::getStatus, status);
        }

        Page<Order> result = orderMapper.selectPage(new Page<>(page, size), wrapper);
        return toPage(result);
    }

    @Override
    public PageResult<OrderBriefResponse> listShopOrders(Long shopId, Long merchantId, String status,
                                                         long page, long size) {
        requireShopOwner(shopId, merchantId);

        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<Order>()
                .eq(Order::getShopId, shopId)
                .orderByDesc(Order::getCreatedAt);
        if (StringUtils.hasText(status)) {
            wrapper.eq(Order::getStatus, status);
        }

        Page<Order> result = orderMapper.selectPage(new Page<>(page, size), wrapper);
        return toPage(result);
    }

    @Override
    public void cancel(Long orderId, Long userId) {
        orderTxService.cancel(orderId, userId);
    }

    @Override
    public void pay(Long orderId, Long userId) {
        orderTxService.pay(orderId, userId);
    }

    @Override
    public void accept(Long orderId, Long merchantId) {
        orderTxService.accept(orderId, merchantId);
        notifyUser(orderId, "ORDER_ACCEPTED", "订单已接单", "商户已接单,请留意配送");
    }

    @Override
    public void complete(Long orderId, Long merchantId) {
        orderTxService.complete(orderId, merchantId);
        notifyUser(orderId, "ORDER_COMPLETED", "订单已完成", "订单已完成,感谢您的光临");
    }

    private void notifyMerchant(Order order) {
        Shop shop = shopService.getById(order.getShopId());
        if (shop == null) {
            return;
        }
        Merchant merchant = merchantService.getById(shop.getMerchantId());
        if (merchant == null) {
            return;
        }
        notificationService.send(
                merchant.getUserId(),
                "ORDER_CREATED",
                "新订单",
                "您有一笔新订单,请及时处理",
                String.valueOf(order.getId()));
    }

    private void notifyUser(Long orderId, String type, String title, String content) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            return;
        }
        notificationService.send(
                order.getUserId(), type, title, content, String.valueOf(orderId));
    }

    @Override
    public void closeIfTimeout(Long orderId) {
        orderTxService.closeIfTimeout(orderId);
    }

    @Override
    public void closeTimeoutOrders() {
        LocalDateTime deadline = LocalDateTime.now().minus(orderProperties.timeout());
        List<Order> orders = orderMapper.selectList(new LambdaQueryWrapper<Order>()
                .eq(Order::getStatus, OrderStatus.CREATED.name())
                .lt(Order::getCreatedAt, deadline)
                .last("LIMIT " + SCAN_BATCH_SIZE));

        for (Order order : orders) {
            orderTxService.closeIfTimeout(order.getId());
        }
    }

    private Order requireOrder(Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
        }
        return order;
    }

    private List<OrderItem> listItems(Long orderId) {
        return orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, orderId));
    }

    private boolean isShopOwner(Long shopId, Long merchantId) {
        Shop shop = shopService.getById(shopId);
        return shop != null && shop.getMerchantId().equals(merchantId);
    }

    private void requireShopOwner(Long shopId, Long merchantId) {
        Shop shop = shopService.getById(shopId);
        if (shop == null) {
            throw new BusinessException(ErrorCode.SHOP_NOT_FOUND);
        }
        if (!shop.getMerchantId().equals(merchantId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }

    private PageResult<OrderBriefResponse> toPage(Page<Order> result) {
        List<OrderBriefResponse> list = result.getRecords().stream()
                .map(OrderConverter::toBrief)
                .toList();
        return PageResult.of(list, result.getCurrent(), result.getSize(), result.getTotal());
    }
}
