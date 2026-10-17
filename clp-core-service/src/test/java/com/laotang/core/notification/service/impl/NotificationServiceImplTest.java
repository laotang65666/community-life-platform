package com.laotang.core.notification.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.laotang.common.exception.BusinessException;
import com.laotang.common.exception.ErrorCode;
import com.laotang.core.notification.entity.Notification;
import com.laotang.core.notification.mapper.NotificationMapper;
import com.laotang.core.notification.push.NotificationPushService;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    @Mock
    private NotificationMapper notificationMapper;
    @Mock
    private NotificationPushService pushService;
    @InjectMocks
    private NotificationServiceImpl notificationService;

    @BeforeAll
    static void initTableInfo() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, Notification.class);
    }

    private Notification notification() {
        Notification notification = new Notification();
        notification.setId(10L);
        notification.setUserId(1L);
        notification.setType("ORDER_CREATED");
        notification.setTitle("新订单");
        notification.setContent("您有一笔新订单");
        notification.setBizId("100");
        notification.setIsRead(0);
        notification.setCreatedAt(LocalDateTime.now());
        return notification;
    }

    @Test
    @DisplayName("发送通知委托给推送服务")
    void send_delegates() {
        notificationService.send(1L, "ORDER_CREATED", "新订单", "内容", "100");

        verify(pushService).send(1L, "ORDER_CREATED", "新订单", "内容", "100");
    }

    @Test
    @DisplayName("通知列表")
    void list() {
        Page<Notification> page = new Page<>(1, 10);
        page.setRecords(List.of(notification()));
        page.setTotal(1);
        when(notificationMapper.selectPage(any(), any())).thenReturn(page);

        var result = notificationService.list(1L, 1, 10);

        assertThat(result.total()).isEqualTo(1);
        assertThat(result.list()).hasSize(1);
        assertThat(result.list().getFirst().type()).isEqualTo("ORDER_CREATED");
    }

    @Test
    @DisplayName("未读数量")
    void unreadCount() {
        when(notificationMapper.selectCount(any())).thenReturn(3L);

        assertThat(notificationService.unreadCount(1L)).isEqualTo(3);
    }

    @Test
    @DisplayName("通知不存在 -> 9003")
    void markRead_notFound() {
        when(notificationMapper.update(isNull(), any())).thenReturn(0);

        BusinessException ex = catchThrowableOfType(
                () -> notificationService.markRead(1L, 99L), BusinessException.class);

        assertThat(ex).isNotNull();
        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.NOTIFICATION_NOT_FOUND);
    }

    @Test
    @DisplayName("全部标记已读")
    void markAllRead() {
        notificationService.markAllRead(1L);
        verify(notificationMapper).update(isNull(), any());
    }
}
