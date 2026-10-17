package com.laotang.core.order.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.laotang.common.exception.BusinessException;
import com.laotang.common.exception.ErrorCode;
import com.laotang.core.order.entity.Order;
import com.laotang.core.order.entity.OrderItem;
import com.laotang.core.order.mapper.OrderItemMapper;
import com.laotang.core.order.mapper.OrderMapper;
import com.laotang.core.product.entity.Product;
import com.laotang.core.product.mapper.ProductMapper;
import com.laotang.core.shop.entity.Shop;
import com.laotang.core.shop.service.IShopService;
import com.laotang.core.stats.event.OrderEventPublisher;
import com.laotang.id.SnowflakeIdGenerator;
import com.laotang.parameter.order.request.CreateOrderRequest;
import com.laotang.parameter.order.request.OrderItemRequest;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderTxServiceTest {

    @Mock
    private OrderMapper orderMapper;
    @Mock
    private OrderItemMapper orderItemMapper;
    @Mock
    private ProductMapper productMapper;
    @Mock
    private IShopService shopService;
    @Mock
    private SnowflakeIdGenerator snowflakeIdGenerator;
    @Mock
    private OrderEventPublisher orderEventPublisher;
    @InjectMocks
    private OrderTxService orderTxService;

    @BeforeAll
    static void initTableInfo() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, Order.class);
    }

    private Shop shop() {
        Shop shop = new Shop();
        shop.setId(10L);
        shop.setMerchantId(1L);
        shop.setStatus("OPEN");
        return shop;
    }

    private Product product() {
        Product product = new Product();
        product.setId(100L);
        product.setShopId(10L);
        product.setName("手工水饺");
        product.setImage("");
        product.setPrice(1990L);
        product.setStock(100);
        product.setStatus("ON_SALE");
        return product;
    }

    private CreateOrderRequest request(int quantity) {
        return new CreateOrderRequest(10L,
                List.of(new OrderItemRequest(100L, quantity)), "少放辣");
    }

    @Test
    @DisplayName("库存不足 -> 5003")
    void create_stockInsufficient() {
        when(shopService.getById(10L)).thenReturn(shop());
        when(productMapper.selectById(100L)).thenReturn(product());
        when(productMapper.deductStock(100L, 10L, 2)).thenReturn(0);

        BusinessException ex = catchThrowableOfType(
                () -> orderTxService.create(1L, request(2)), BusinessException.class);

        assertThat(ex).isNotNull();
        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.STOCK_NOT_ENOUGH);
    }

    @Test
    @DisplayName("下单成功,总额 = 单价 × 数量")
    void create_success() {
        when(shopService.getById(10L)).thenReturn(shop());
        when(productMapper.selectById(100L)).thenReturn(product());
        when(productMapper.deductStock(100L, 10L, 2)).thenReturn(1);
        when(snowflakeIdGenerator.nextId()).thenReturn(123456789L);
        when(orderMapper.insert(any(Order.class))).thenAnswer(inv -> {
            Order order = inv.getArgument(0);
            order.setId(500L);
            return 1;
        });

        OrderTxService.CreatedOrder created = orderTxService.create(1L, request(2));

        assertThat(created.order().getId()).isEqualTo(500L);
        assertThat(created.order().getTotalAmount()).isEqualTo(3980L);
        assertThat(created.order().getStatus()).isEqualTo("CREATED");
        assertThat(created.items()).hasSize(1);
        assertThat(created.items().getFirst().getAmount()).isEqualTo(3980L);
        assertThat(created.items().getFirst().getOrderId()).isEqualTo(500L);
    }

    @Test
    @DisplayName("非 CREATED 状态不能支付 -> 6002")
    void pay_statusConflict() {
        Order order = new Order();
        order.setId(500L);
        order.setUserId(1L);
        order.setStatus("PAID");
        when(orderMapper.selectById(500L)).thenReturn(order);
        when(orderMapper.update(isNull(), any())).thenReturn(0);

        BusinessException ex = catchThrowableOfType(
                () -> orderTxService.pay(500L, 1L), BusinessException.class);

        assertThat(ex).isNotNull();
        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.ORDER_STATUS_CONFLICT);
    }

    @Test
    @DisplayName("取消订单成功后回滚库存")
    void cancel_restoresStock() {
        Order order = new Order();
        order.setId(500L);
        order.setUserId(1L);
        order.setShopId(10L);
        order.setStatus("CREATED");
        when(orderMapper.selectById(500L)).thenReturn(order);
        when(orderMapper.update(isNull(), any())).thenReturn(1);

        OrderItem item = new OrderItem();
        item.setProductId(100L);
        item.setQuantity(2);
        when(orderItemMapper.selectList(any())).thenReturn(List.of(item));

        orderTxService.cancel(500L, 1L);

        verify(productMapper).restoreStock(100L, 2);
    }
}
