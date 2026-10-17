package com.laotang.core.order.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.laotang.common.exception.BusinessException;
import com.laotang.common.exception.ErrorCode;
import com.laotang.core.order.entity.Order;
import com.laotang.core.order.entity.OrderItem;
import com.laotang.core.order.enums.OrderStatus;
import com.laotang.core.order.mapper.OrderItemMapper;
import com.laotang.core.order.mapper.OrderMapper;
import com.laotang.core.product.entity.Product;
import com.laotang.core.product.mapper.ProductMapper;
import com.laotang.core.shop.entity.Shop;
import com.laotang.core.shop.service.IShopService;
import com.laotang.core.stats.event.OrderEventPublisher;
import com.laotang.core.stats.event.OrderEventType;
import com.laotang.id.SnowflakeIdGenerator;
import com.laotang.parameter.order.request.CreateOrderRequest;
import com.laotang.parameter.order.request.OrderItemRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 订单事务操作。
 *
 * <p>所有需要保证原子性的写操作都放在这里,由 {@link com.laotang.core.order.service.impl.OrderServiceImpl}
 * 负责幂等、延时队列等非事务编排。</p>
 */
@Service
@RequiredArgsConstructor
public class OrderTxService {

    private static final String SHOP_OPEN = "OPEN";
    private static final String PRODUCT_ON_SALE = "ON_SALE";

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final ProductMapper productMapper;
    private final IShopService shopService;
    private final SnowflakeIdGenerator snowflakeIdGenerator;
    private final OrderEventPublisher orderEventPublisher;

    @Transactional(rollbackFor = Exception.class)
    public CreatedOrder create(Long userId, CreateOrderRequest request) {
        Shop shop = shopService.getById(request.shopId());
        if (shop == null) {
            throw new BusinessException(ErrorCode.SHOP_NOT_FOUND);
        }
        if (!SHOP_OPEN.equals(shop.getStatus())) {
            throw new BusinessException(ErrorCode.SHOP_CLOSED);
        }

        long totalAmount = 0L;
        List<OrderItem> items = new ArrayList<>(request.items().size());

        for (OrderItemRequest itemRequest : request.items()) {
            Product product = productMapper.selectById(itemRequest.productId());
            if (product == null || !product.getShopId().equals(request.shopId())) {
                throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND);
            }
            if (!PRODUCT_ON_SALE.equals(product.getStatus())) {
                throw new BusinessException(ErrorCode.PRODUCT_OFF_SALE);
            }

            // 条件扣库存:stock >= qty + 上架状态 + 店铺归属,一条 SQL 完成
            int deducted = productMapper.deductStock(
                    product.getId(), request.shopId(), itemRequest.quantity());
            if (deducted == 0) {
                Product latest = productMapper.selectById(product.getId());
                if (latest == null) {
                    throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND);
                }
                if (!PRODUCT_ON_SALE.equals(latest.getStatus())) {
                    throw new BusinessException(ErrorCode.PRODUCT_OFF_SALE);
                }
                throw new BusinessException(ErrorCode.STOCK_NOT_ENOUGH);
            }

            long amount = Math.multiplyExact(product.getPrice(), itemRequest.quantity().longValue());
            totalAmount = Math.addExact(totalAmount, amount);

            OrderItem item = new OrderItem();
            item.setProductId(product.getId());
            item.setProductName(product.getName());
            item.setProductImage(product.getImage());
            item.setPrice(product.getPrice());
            item.setQuantity(itemRequest.quantity());
            item.setAmount(amount);
            items.add(item);
        }

        Order order = new Order();
        order.setOrderNo(String.valueOf(snowflakeIdGenerator.nextId()));
        order.setUserId(userId);
        order.setShopId(request.shopId());
        order.setTotalAmount(totalAmount);
        order.setStatus(OrderStatus.CREATED.name());
        order.setRemark(request.remark() == null ? "" : request.remark());
        // 显式设置,订单事件需要用它决定统计归属日期
        order.setCreatedAt(LocalDateTime.now());
        orderMapper.insert(order);

        for (OrderItem item : items) {
            item.setOrderId(order.getId());
            orderItemMapper.insert(item);
        }

        // 与业务写入同事务:订单成功则事件一定进 Outbox
        orderEventPublisher.publish(OrderEventType.ORDER_CREATED, order);

        return new CreatedOrder(order, items);
    }

    @Transactional(rollbackFor = Exception.class)
    public void pay(Long orderId, Long userId) {
        Order order = requireOrder(orderId);
        requireOwner(order, userId);

        int rows = orderMapper.update(null, new LambdaUpdateWrapper<Order>()
                .eq(Order::getId, orderId)
                .eq(Order::getStatus, OrderStatus.CREATED.name())
                .set(Order::getStatus, OrderStatus.PAID.name())
                .set(Order::getPayTime, LocalDateTime.now()));
        if (rows == 0) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_CONFLICT);
        }
        orderEventPublisher.publish(OrderEventType.ORDER_PAID, order);
    }

    @Transactional(rollbackFor = Exception.class)
    public void cancel(Long orderId, Long userId) {
        Order order = requireOrder(orderId);
        requireOwner(order, userId);

        int rows = orderMapper.update(null, new LambdaUpdateWrapper<Order>()
                .eq(Order::getId, orderId)
                .eq(Order::getStatus, OrderStatus.CREATED.name())
                .set(Order::getStatus, OrderStatus.CANCELLED.name())
                .set(Order::getCancelTime, LocalDateTime.now())
                .set(Order::getCancelReason, "用户取消"));
        if (rows == 0) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_CONFLICT);
        }
        restoreStock(orderId);
        orderEventPublisher.publish(OrderEventType.ORDER_CANCELLED, order);
    }

    @Transactional(rollbackFor = Exception.class)
    public void accept(Long orderId, Long merchantId) {
        Order order = requireOrder(orderId);
        requireShopOwner(order.getShopId(), merchantId);

        int rows = orderMapper.update(null, new LambdaUpdateWrapper<Order>()
                .eq(Order::getId, orderId)
                .eq(Order::getStatus, OrderStatus.PAID.name())
                .set(Order::getStatus, OrderStatus.ACCEPTED.name())
                .set(Order::getAcceptTime, LocalDateTime.now()));
        if (rows == 0) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_CONFLICT);
        }
        orderEventPublisher.publish(OrderEventType.ORDER_ACCEPTED, order);
    }

    @Transactional(rollbackFor = Exception.class)
    public void complete(Long orderId, Long merchantId) {
        Order order = requireOrder(orderId);
        requireShopOwner(order.getShopId(), merchantId);

        int rows = orderMapper.update(null, new LambdaUpdateWrapper<Order>()
                .eq(Order::getId, orderId)
                .eq(Order::getStatus, OrderStatus.ACCEPTED.name())
                .set(Order::getStatus, OrderStatus.COMPLETED.name())
                .set(Order::getCompleteTime, LocalDateTime.now()));
        if (rows == 0) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_CONFLICT);
        }
        orderEventPublisher.publish(OrderEventType.ORDER_COMPLETED, order);
    }

    /**
     * 关闭超时订单。条件更新保证幂等:只有 CREATED 才会被关闭。
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean closeIfTimeout(Long orderId) {
        int rows = orderMapper.update(null, new LambdaUpdateWrapper<Order>()
                .eq(Order::getId, orderId)
                .eq(Order::getStatus, OrderStatus.CREATED.name())
                .set(Order::getStatus, OrderStatus.CLOSED.name())
                .set(Order::getCancelTime, LocalDateTime.now())
                .set(Order::getCancelReason, "超时未支付"));
        if (rows == 0) {
            return false;
        }
        restoreStock(orderId);
        Order order = orderMapper.selectById(orderId);
        if (order != null) {
            orderEventPublisher.publish(OrderEventType.ORDER_CLOSED, order);
        }
        return true;
    }

    private void restoreStock(Long orderId) {
        List<OrderItem> items = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, orderId));
        for (OrderItem item : items) {
            productMapper.restoreStock(item.getProductId(), item.getQuantity());
        }
    }

    private Order requireOrder(Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
        }
        return order;
    }

    private void requireOwner(Order order, Long userId) {
        if (!order.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
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

    public record CreatedOrder(Order order, List<OrderItem> items) {
    }
}
