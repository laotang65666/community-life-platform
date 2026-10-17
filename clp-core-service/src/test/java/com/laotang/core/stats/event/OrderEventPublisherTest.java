package com.laotang.core.stats.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.laotang.core.order.entity.Order;
import com.laotang.core.outbox.service.OutboxService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OrderEventPublisherTest {

    @Mock
    private OutboxService outboxService;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Test
    @DisplayName("发布订单事件:写入 Outbox,事件 ID 与 messageId 一致")
    void publish_enqueuesOutbox() throws Exception {
        OrderEventPublisher publisher = new OrderEventPublisher(outboxService, objectMapper);
        Order order = new Order();
        order.setId(500L);
        order.setOrderNo("ON-500");
        order.setUserId(1L);
        order.setShopId(10L);
        order.setTotalAmount(3980L);
        order.setCreatedAt(LocalDateTime.of(2026, 10, 17, 10, 0));

        publisher.publish(OrderEventType.ORDER_PAID, order);

        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> eventIdCaptor = ArgumentCaptor.forClass(String.class);
        verify(outboxService).enqueue(
                eq(OrderEventPublisher.TOPIC),
                eq(OrderEventPublisher.BIZ_TYPE),
                eq("500"),
                payloadCaptor.capture(),
                eventIdCaptor.capture());

        OrderEvent event = objectMapper.readValue(payloadCaptor.getValue(), OrderEvent.class);
        assertThat(event.eventId()).isEqualTo(eventIdCaptor.getValue());
        assertThat(event.eventType()).isEqualTo("ORDER_PAID");
        assertThat(event.orderId()).isEqualTo(500L);
        assertThat(event.shopId()).isEqualTo(10L);
        assertThat(event.amount()).isEqualTo(3980L);
        assertThat(event.orderCreatedAt()).isEqualTo(LocalDateTime.of(2026, 10, 17, 10, 0));
    }
}
