package com.laotang.core.shop.service.impl;

import com.laotang.common.exception.BusinessException;
import com.laotang.common.exception.ErrorCode;
import com.laotang.core.shop.entity.Shop;
import com.laotang.core.shop.mapper.ShopMapper;
import com.laotang.core.shop.mapper.ShopTypeMapper;
import com.laotang.framework.redis.cache.CacheProperties;
import com.laotang.framework.redis.cache.MultiLevelCache;
import com.laotang.parameter.shop.request.UpdateShopRequest;
import com.laotang.parameter.shop.response.ShopResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ShopServiceImplTest {

    @Mock
    private ShopMapper shopMapper;
    @Mock
    private ShopTypeMapper shopTypeMapper;
    @Mock
    private MultiLevelCache cache;
    @Mock
    private CacheProperties cacheProperties;
    @InjectMocks
    private ShopServiceImpl shopService;

    @Test
    @SuppressWarnings("unchecked")
    @DisplayName("店铺不存在 -> 4006")
    void getDetail_notFound() {
        when(shopMapper.selectById(99L)).thenReturn(null);
        when(cache.getOrLoad(anyString(), eq(ShopResponse.class), any(), any()))
                .thenAnswer(inv -> ((Supplier<ShopResponse>) inv.getArgument(3)).get());

        BusinessException ex = catchThrowableOfType(
                () -> shopService.getDetail(99L), BusinessException.class);

        assertThat(ex).isNotNull();
        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.SHOP_NOT_FOUND);
    }

    @Test
    @SuppressWarnings("unchecked")
    @DisplayName("店铺详情:缓存未命中时回源")
    void getDetail_success() {
        Shop shop = new Shop();
        shop.setId(10L);
        shop.setMerchantId(1L);
        shop.setName("测试店铺");
        shop.setStatus("OPEN");
        when(shopMapper.selectById(10L)).thenReturn(shop);
        when(cache.getOrLoad(anyString(), eq(ShopResponse.class), any(), any()))
                .thenAnswer(inv -> ((Supplier<ShopResponse>) inv.getArgument(3)).get());

        ShopResponse response = shopService.getDetail(10L);

        assertThat(response.id()).isEqualTo("10");
        assertThat(response.name()).isEqualTo("测试店铺");
    }

    @Test
    @DisplayName("非店主修改店铺 -> 2003")
    void update_notOwner() {
        Shop shop = new Shop();
        shop.setId(10L);
        shop.setMerchantId(1L);
        when(shopMapper.selectById(10L)).thenReturn(shop);

        BusinessException ex = catchThrowableOfType(
                () -> shopService.update(10L, 2L, new UpdateShopRequest("新名字", null, null, null, null, null)),
                BusinessException.class);

        assertThat(ex).isNotNull();
        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN);
    }

    @Test
    @DisplayName("全部字段为空时不执行更新")
    void update_noChanges() {
        Shop shop = new Shop();
        shop.setId(10L);
        shop.setMerchantId(1L);
        when(shopMapper.selectById(10L)).thenReturn(shop);

        shopService.update(10L, 1L, new UpdateShopRequest(null, null, null, null, null, null));

        verify(shopMapper, never()).update(any(), any());
    }

    @Test
    @DisplayName("分类不存在时不能创建店铺 -> 1001")
    void createForMerchant_typeNotFound() {
        when(shopTypeMapper.selectById(99L)).thenReturn(null);

        com.laotang.core.merchant.entity.Merchant merchant = new com.laotang.core.merchant.entity.Merchant();
        merchant.setId(10L);

        BusinessException ex = catchThrowableOfType(
                () -> shopService.createForMerchant(merchant, 99L), BusinessException.class);

        assertThat(ex).isNotNull();
        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.PARAM_INVALID);
    }
}
