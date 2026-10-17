package com.laotang.core.shop.service;

import com.laotang.common.result.PageResult;
import com.laotang.core.merchant.entity.Merchant;
import com.laotang.core.shop.entity.Shop;
import com.laotang.parameter.shop.request.UpdateShopRequest;
import com.laotang.parameter.shop.response.ShopBriefResponse;
import com.laotang.parameter.shop.response.ShopResponse;
import com.laotang.parameter.shop.response.ShopTypeResponse;

import java.util.List;

/**
 * 店铺服务。
 */
public interface IShopService {

    PageResult<ShopBriefResponse> list(long page, long size, Long typeId, String keyword);

    ShopResponse getDetail(Long shopId);

    /**
     * 按 ID 获取店铺实体;不存在返回 null。供其他业务域做归属校验。
     */
    Shop getById(Long shopId);

    /**
     * 按商户 ID 查店铺;一个商户只对应一家店。不存在返回 null。
     */
    Shop getByMerchantId(Long merchantId);

    void update(Long shopId, Long merchantId, UpdateShopRequest request);

    /**
     * 审核通过时为商户创建店铺。已存在则直接返回(幂等)。
     */
    Shop createForMerchant(Merchant merchant, Long typeId);

    List<ShopTypeResponse> listTypes();
}
