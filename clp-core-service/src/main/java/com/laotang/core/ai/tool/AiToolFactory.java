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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 按请求创建工具实例。
 *
 * <p>工具持有本次请求的用户身份,所以不能做成单例 Bean;
 * 这里用工厂把身份解析和依赖装配集中在一处。</p>
 */
@Component
@RequiredArgsConstructor
public class AiToolFactory {

    private final IOrderService orderService;
    private final IProductService productService;
    private final IShopService shopService;
    private final IMerchantService merchantService;
    private final IGrowthService growthService;

    public List<Object> create(AiScope scope, Long userId, AiToolRecorder recorder) {
        return switch (scope) {
            case USER -> List.of(new UserAiTools(
                    userId, orderService, productService, shopService, growthService, recorder));
            case MERCHANT -> List.of(createMerchantTools(userId, recorder));
        };
    }

    private MerchantAiTools createMerchantTools(Long userId, AiToolRecorder recorder) {
        Merchant merchant = merchantService.getApprovedByUserId(userId);
        if (merchant == null) {
            throw new BusinessException(ErrorCode.MERCHANT_NOT_APPROVED);
        }
        Shop shop = shopService.getByMerchantId(merchant.getId());
        if (shop == null) {
            throw new BusinessException(ErrorCode.SHOP_NOT_FOUND);
        }
        return new MerchantAiTools(
                merchant.getId(),
                shop.getId(),
                orderService,
                productService,
                shopService,
                recorder);
    }
}
