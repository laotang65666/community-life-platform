package com.laotang.core.order.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.laotang.common.exception.BusinessException;
import com.laotang.common.exception.ErrorCode;
import com.laotang.core.idempotency.entity.IdempotencyRecord;
import com.laotang.core.idempotency.service.IdempotencyService;
import com.laotang.core.order.config.OrderProperties;
import com.laotang.core.order.entity.Order;
import com.laotang.core.order.mapper.OrderItemMapper;
import com.laotang.core.order.mapper.OrderMapper;
import com.laotang.core.order.service.OrderTxService;
import com.laotang.core.shop.service.IShopService;
import com.laotang.framework.lock.delay.RedissonDelayQueue;
import com.laotang.parameter.order.request.CreateOrderRequest;
import com.laotang.parameter.order.request.OrderItemRequest;
import com.laotang.parameter.order.response.OrderResponse;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock
    private OrderMapper orderMapper;
    @Mock
    private OrderItemMapper orderItemMapper;
    @Mock
    private OrderTxService orderTxService;
    @Mock
    private IdempotencyService idempotencyService;
    @Mock
    private RedissonDelayQueue delayQueue;
    @Mock
    private IShopService shopService;

    @org.mockito.Spy
    private OrderProperties orderProperties = new OrderProperties(Duration.ofMinutes(30));

    @InjectMocks
    private OrderServiceImpl orderService;

    @BeforeAll
    static void initTableInfo() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, Order.class);
    }

    private CreateOrderRequest request() {
        return new CreateOrderRequest(10L,
                List.of(new OrderItemRequest(100L, 2)), "少放辣");
    }

    private Order order() {
        Order order = new Order();
        order.setId(500L);
        order.setOrderNo("ON-500");
        order.setUserId(1L);
        order.setShopId(10L);
        order.setTotalAmount(3980L);
        order.setStatus("CREATED");
        return order;
    }

    @Test
    @DisplayName("幂等键处理中 -> 1005")
    void create_processing() {
        when(idempotencyService.tryAcquire(1L, "key-1", "ORDER")).thenReturn(null);
        IdempotencyRecord existing = new IdempotencyRecord();
        existing.setStatus(IdempotencyService.PROCESSING);
        when(idempotencyService.find(1L, "key-1")).thenReturn(existing);

        BusinessException ex = catchThrowableOfType(
                () -> orderService.create(1L, "key-1", request()), BusinessException.class);

        assertThat(ex).isNotNull();
        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.REQUEST_PROCESSING);
    }

    @Test
    @DisplayName("幂等键已成功 -> 返回首次订单,不再下单")
    void create_duplicateSuccess() {
        when(idempotencyService.tryAcquire(1L, "key-1", "ORDER")).thenReturn(null);
        IdempotencyRecord existing = new IdempotencyRecord();
        existing.setStatus(IdempotencyService.SUCCESS);
        existing.setBizId("500");
        when(idempotencyService.find(1L, "key-1")).thenReturn(existing);
        when(orderMapper.selectById(500L)).thenReturn(order());
        when(orderItemMapper.selectList(any())).thenReturn(List.of());

        OrderResponse response = orderService.create(1L, "key-1", request());

        assertThat(response.id()).isEqualTo("500");
        assertThat(response.totalAmount()).isEqualTo(3980L);
    }

    @Test
    @DisplayName("首次下单 -> 标记幂等成功并投递延时关单消息")
    void create_success() {
        IdempotencyRecord acquired = new IdempotencyRecord();
        acquired.setId(1L);
        when(idempotencyService.tryAcquire(1L, "key-1", "ORDER")).thenReturn(acquired);

        Order order = order();
        when(orderTxService.create(1L, request()))
                .thenReturn(new OrderTxService.CreatedOrder(order, List.of()));

        orderService.create(1L, "key-1", request());

        verify(idempotencyService).markSuccess(1L, 500L);
        verify(delayQueue).offer(anyString(), eq("500"), eq(Duration.ofMinutes(30).toMillis()),
                eq(TimeUnit.MILLISECONDS));
    }

    @Test
    @DisplayName("定时扫描:超时订单交给事务服务关闭")
    void closeTimeoutOrders() {
        when(orderMapper.selectList(any())).thenReturn(List.of(order()));

        orderService.closeTimeoutOrders();

        verify(orderTxService).closeIfTimeout(500L);
    }
}
