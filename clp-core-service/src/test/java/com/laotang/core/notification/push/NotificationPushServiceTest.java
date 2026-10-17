package com.laotang.core.notification.push;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.laotang.core.notification.entity.Notification;
import com.laotang.core.notification.mapper.NotificationMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationPushServiceTest {

    @Mock
    private NotificationMapper notificationMapper;
    @Mock
    private RedissonClient redisson;
    @Mock
    private RTopic topic;

    private NotificationPushService pushService;

    @BeforeEach
    void setUp() {
        pushService = new NotificationPushService(notificationMapper, redisson, new ObjectMapper());
    }

    @Test
    @DisplayName("保存通知并广播到 Redis 频道")
    void send_storesAndPublishes() {
        when(notificationMapper.insert(any(Notification.class))).thenAnswer(inv -> {
            Notification notification = inv.getArgument(0);
            notification.setId(10L);
            return 1;
        });
        when(redisson.getTopic(anyString(), any())).thenReturn(topic);

        pushService.send(1L, "ORDER_CREATED", "新订单", "您有一笔新订单", "100");

        verify(notificationMapper).insert(any(Notification.class));
        verify(topic).publish(anyString());
    }
}
