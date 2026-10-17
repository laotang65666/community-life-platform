package com.laotang.core.shop.init;

import com.laotang.core.shop.entity.ShopType;
import com.laotang.core.shop.mapper.ShopTypeMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ShopTypeInitializerTest {

    @Mock
    private ShopTypeMapper shopTypeMapper;
    @InjectMocks
    private ShopTypeInitializer initializer;

    @Test
    @DisplayName("只补齐缺失的默认分类")
    void run_insertsMissingTypesOnly() {
        ShopType existing = new ShopType();
        existing.setName("美食");
        when(shopTypeMapper.selectList(isNull())).thenReturn(List.of(existing));

        initializer.run();

        ArgumentCaptor<ShopType> captor = ArgumentCaptor.forClass(ShopType.class);
        verify(shopTypeMapper, times(7)).insert(captor.capture());
        assertThat(captor.getAllValues())
                .extracting(ShopType::getName)
                .doesNotContain("美食")
                .contains("休闲娱乐", "生活服务", "购物", "酒店", "丽人", "运动健身", "教育培训");
    }
}
