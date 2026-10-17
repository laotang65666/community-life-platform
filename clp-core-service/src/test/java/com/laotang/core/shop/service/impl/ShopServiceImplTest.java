package com.laotang.core.shop.service.impl;

import com.laotang.common.exception.BusinessException;
import com.laotang.common.exception.ErrorCode;
import com.laotang.core.shop.entity.Shop;
import com.laotang.core.shop.mapper.ShopMapper;
import com.laotang.core.shop.mapper.ShopTypeMapper;
import com.laotang.parameter.shop.request.UpdateShopRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
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
    @InjectMocks
    private ShopServiceImpl shopService;

    @Test
    @DisplayName("店铺不存在 -> 4006")
    void getDetail_notFound() {
        when(shopMapper.selectById(99L)).thenReturn(null);

        BusinessException ex = catchThrowableOfType(
                () -> shopService.getDetail(99L), BusinessException.class);

        assertThat(ex).isNotNull();
        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.SHOP_NOT_FOUND);
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
