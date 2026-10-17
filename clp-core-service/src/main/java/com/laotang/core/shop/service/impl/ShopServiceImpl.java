package com.laotang.core.shop.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.laotang.common.constant.RedisKey;
import com.laotang.common.exception.BusinessException;
import com.laotang.common.exception.ErrorCode;
import com.laotang.common.result.PageResult;
import com.laotang.framework.redis.cache.CacheProperties;
import com.laotang.framework.redis.cache.MultiLevelCache;
import com.laotang.core.merchant.entity.Merchant;
import com.laotang.core.shop.converter.ShopConverter;
import com.laotang.core.shop.entity.Shop;
import com.laotang.core.shop.entity.ShopType;
import com.laotang.core.shop.mapper.ShopMapper;
import com.laotang.core.shop.mapper.ShopTypeMapper;
import com.laotang.core.shop.service.IShopService;
import com.laotang.parameter.shop.request.UpdateShopRequest;
import com.laotang.parameter.shop.response.ShopBriefResponse;
import com.laotang.parameter.shop.response.ShopResponse;
import com.laotang.parameter.shop.response.ShopTypeResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 店铺服务实现。
 */
@Service
@RequiredArgsConstructor
public class ShopServiceImpl implements IShopService {

    private static final String STATUS_OPEN = "OPEN";

    private final ShopMapper shopMapper;
    private final ShopTypeMapper shopTypeMapper;
    private final MultiLevelCache cache;
    private final CacheProperties cacheProperties;

    @Override
    public PageResult<ShopBriefResponse> list(long page, long size, Long typeId, String keyword) {
        LambdaQueryWrapper<Shop> wrapper = new LambdaQueryWrapper<Shop>()
                .eq(Shop::getStatus, STATUS_OPEN);

        if (typeId != null) {
            wrapper.eq(Shop::getTypeId, typeId);
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.like(Shop::getName, keyword);
        }
        wrapper.orderByDesc(Shop::getCreatedAt);

        Page<Shop> result = shopMapper.selectPage(new Page<>(page, size), wrapper);
        List<ShopBriefResponse> list = result.getRecords().stream()
                .map(ShopConverter::toBrief)
                .toList();
        return PageResult.of(list, result.getCurrent(), result.getSize(), result.getTotal());
    }

    @Override
    public ShopResponse getDetail(Long shopId) {
        String cacheKey = RedisKey.cacheShop(shopId);
        ShopResponse cached = cache.get(cacheKey, ShopResponse.class);
        if (cached != null) {
            return cached;
        }

        Shop shop = shopMapper.selectById(shopId);
        if (shop == null) {
            throw new BusinessException(ErrorCode.SHOP_NOT_FOUND);
        }
        ShopResponse response = ShopConverter.toResponse(shop);
        cache.put(cacheKey, response, cacheProperties.getShopTtl());
        return response;
    }

    @Override
    public Shop getById(Long shopId) {
        return shopMapper.selectById(shopId);
    }

    @Override
    public Shop getByMerchantId(Long merchantId) {
        return shopMapper.selectOne(new LambdaQueryWrapper<Shop>()
                .eq(Shop::getMerchantId, merchantId)
                .last("LIMIT 1"));
    }

    @Override
    public void update(Long shopId, Long merchantId, UpdateShopRequest request) {
        Shop shop = shopMapper.selectById(shopId);
        if (shop == null) {
            throw new BusinessException(ErrorCode.SHOP_NOT_FOUND);
        }
        if (!shop.getMerchantId().equals(merchantId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        LambdaUpdateWrapper<Shop> wrapper = new LambdaUpdateWrapper<Shop>()
                .eq(Shop::getId, shopId);

        boolean changed = false;
        if (request.name() != null) {
            wrapper.set(Shop::getName, request.name());
            changed = true;
        }
        if (request.address() != null) {
            wrapper.set(Shop::getAddress, request.address());
            changed = true;
        }
        if (request.phone() != null) {
            wrapper.set(Shop::getPhone, request.phone());
            changed = true;
        }
        if (request.openTime() != null) {
            wrapper.set(Shop::getOpenTime, request.openTime());
            changed = true;
        }
        if (request.closeTime() != null) {
            wrapper.set(Shop::getCloseTime, request.closeTime());
            changed = true;
        }
        if (request.status() != null) {
            wrapper.set(Shop::getStatus, request.status());
            changed = true;
        }

        if (changed) {
            shopMapper.update(null, wrapper);
            cache.evict(RedisKey.cacheShop(shopId));
        }
    }

    @Override
    public Shop createForMerchant(Merchant merchant, Long typeId) {
        if (typeId == null || shopTypeMapper.selectById(typeId) == null) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "店铺分类不存在");
        }

        Shop existing = shopMapper.selectOne(new LambdaQueryWrapper<Shop>()
                .eq(Shop::getMerchantId, merchant.getId()));
        if (existing != null) {
            return existing;
        }

        Shop shop = new Shop();
        shop.setMerchantId(merchant.getId());
        shop.setTypeId(typeId);
        shop.setName(merchant.getShopName());
        shop.setAddress(merchant.getAddress());
        shop.setPhone(merchant.getContactPhone());
        shop.setOpenTime("");
        shop.setCloseTime("");
        shop.setStatus(STATUS_OPEN);

        try {
            shopMapper.insert(shop);
            return shop;
        } catch (DuplicateKeyException e) {
            return shopMapper.selectOne(new LambdaQueryWrapper<Shop>()
                    .eq(Shop::getMerchantId, merchant.getId()));
        }
    }

    @Override
    public List<ShopTypeResponse> listTypes() {
        return shopTypeMapper.selectList(new LambdaQueryWrapper<ShopType>()
                        .orderByAsc(ShopType::getSort))
                .stream()
                .map(ShopConverter::toTypeResponse)
                .toList();
    }
}
