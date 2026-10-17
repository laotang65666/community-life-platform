package com.laotang.core.ai.tool;

import com.laotang.common.exception.BusinessException;
import com.laotang.common.exception.ErrorCode;
import com.laotang.core.ai.enums.AiScope;
import com.laotang.core.growth.service.IGrowthService;
import com.laotang.core.merchant.entity.Merchant;
import com.laotang.core.merchant.service.IMerchantService;
import com.laotang.core.order.service.IOrderService;
import com.laotang.core.product.service.IProductService;
import com.laotang.core.shop.entity.Shop;
import com.laotang.core.shop.service.IShopService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiToolFactoryTest {

    @Mock
    private IOrderService orderService;
    @Mock
    private IProductService productService;
    @Mock
    private IShopService shopService;
    @Mock
    private IMerchantService merchantService;
    @Mock
    private IGrowthService growthService;

    private AiToolFactory factory;

    @BeforeEach
    void setUp() {
        factory = new AiToolFactory(
                orderService, productService, shopService, merchantService, growthService);
    }

    @Test
    @DisplayName("用户端只装配用户工具")
    void create_userScope() {
        var tools = factory.create(AiScope.USER, 1L, new AiToolRecorder());

        assertThat(tools).hasSize(1);
        assertThat(tools.getFirst()).isInstanceOf(UserAiTools.class);
    }

    @Test
    @DisplayName("商户端按登录用户反查店铺后装配工具")
    void create_merchantScope() {
        Merchant merchant = new Merchant();
        merchant.setId(10L);
        merchant.setUserId(1L);
        when(merchantService.getApprovedByUserId(1L)).thenReturn(merchant);

        Shop shop = new Shop();
        shop.setId(20L);
        when(shopService.getByMerchantId(10L)).thenReturn(shop);

        var tools = factory.create(AiScope.MERCHANT, 1L, new AiToolRecorder());

        assertThat(tools).hasSize(1);
        assertThat(tools.getFirst()).isInstanceOf(MerchantAiTools.class);
    }

    @Test
    @DisplayName("没有已通过的商户资质 -> 4003")
    void create_merchantWithoutApproval_throws() {
        when(merchantService.getApprovedByUserId(1L)).thenReturn(null);

        BusinessException ex = catchThrowableOfType(
                () -> factory.create(AiScope.MERCHANT, 1L, new AiToolRecorder()),
                BusinessException.class);

        assertThat(ex).isNotNull();
        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.MERCHANT_NOT_APPROVED);
    }

    @Test
    @DisplayName("商户没有店铺 -> 4006")
    void create_merchantWithoutShop_throws() {
        Merchant merchant = new Merchant();
        merchant.setId(10L);
        when(merchantService.getApprovedByUserId(1L)).thenReturn(merchant);
        when(shopService.getByMerchantId(10L)).thenReturn(null);

        BusinessException ex = catchThrowableOfType(
                () -> factory.create(AiScope.MERCHANT, 1L, new AiToolRecorder()),
                BusinessException.class);

        assertThat(ex).isNotNull();
        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.SHOP_NOT_FOUND);
    }
}
