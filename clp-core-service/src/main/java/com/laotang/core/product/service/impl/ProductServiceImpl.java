package com.laotang.core.product.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.laotang.common.constant.RedisKey;
import com.laotang.common.exception.BusinessException;
import com.laotang.common.exception.ErrorCode;
import com.laotang.common.result.PageResult;
import com.laotang.framework.redis.cache.CacheProperties;
import com.laotang.framework.redis.cache.MultiLevelCache;
import com.laotang.core.product.converter.ProductConverter;
import com.laotang.core.product.entity.Product;
import com.laotang.core.product.mapper.ProductMapper;
import com.laotang.core.product.service.IProductService;
import com.laotang.core.shop.entity.Shop;
import com.laotang.core.shop.service.IShopService;
import com.laotang.parameter.product.request.CreateProductRequest;
import com.laotang.parameter.product.request.UpdateProductRequest;
import com.laotang.parameter.product.response.ProductBriefResponse;
import com.laotang.parameter.product.response.ProductResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 商品服务实现。
 */
@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements IProductService {

    private static final String STATUS_ON_SALE = "ON_SALE";

    private final ProductMapper productMapper;
    private final IShopService shopService;
    private final MultiLevelCache cache;
    private final CacheProperties cacheProperties;

    @Override
    public PageResult<ProductBriefResponse> listByShop(Long shopId, long page, long size, String keyword) {
        requireShop(shopId);

        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<Product>()
                .eq(Product::getShopId, shopId)
                .eq(Product::getStatus, STATUS_ON_SALE);

        if (StringUtils.hasText(keyword)) {
            wrapper.like(Product::getName, keyword);
        }
        wrapper.orderByDesc(Product::getCreatedAt);

        Page<Product> result = productMapper.selectPage(new Page<>(page, size), wrapper);
        return toPage(result);
    }

    @Override
    public PageResult<ProductBriefResponse> listManage(Long shopId, Long merchantId, long page, long size) {
        requireOwnedShop(shopId, merchantId);

        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<Product>()
                .eq(Product::getShopId, shopId)
                .orderByDesc(Product::getCreatedAt);

        Page<Product> result = productMapper.selectPage(new Page<>(page, size), wrapper);
        return toPage(result);
    }

    @Override
    public ProductResponse getDetail(Long productId) {
        String cacheKey = RedisKey.cacheProduct(productId);
        ProductResponse cached = cache.get(cacheKey, ProductResponse.class);
        if (cached != null) {
            return cached;
        }

        ProductResponse response = ProductConverter.toResponse(requireProduct(productId));
        cache.put(cacheKey, response, cacheProperties.getProductTtl());
        return response;
    }

    @Override
    public ProductResponse create(Long shopId, Long merchantId, CreateProductRequest request) {
        requireOwnedShop(shopId, merchantId);

        Product product = new Product();
        product.setShopId(shopId);
        product.setName(request.name());
        product.setPrice(request.price());
        product.setStock(request.stock());
        product.setSold(0);
        product.setDescription(request.description() == null ? "" : request.description());
        product.setImage(request.image() == null ? "" : request.image());
        product.setStatus(STATUS_ON_SALE);
        product.setVersion(0);

        productMapper.insert(product);
        return ProductConverter.toResponse(product);
    }

    @Override
    public void update(Long productId, Long merchantId, UpdateProductRequest request) {
        Product product = requireProduct(productId);
        requireOwnedShop(product.getShopId(), merchantId);

        LambdaUpdateWrapper<Product> wrapper = new LambdaUpdateWrapper<Product>()
                .eq(Product::getId, productId);

        boolean changed = false;
        if (request.name() != null) {
            wrapper.set(Product::getName, request.name());
            changed = true;
        }
        if (request.price() != null) {
            wrapper.set(Product::getPrice, request.price());
            changed = true;
        }
        if (request.stock() != null) {
            wrapper.set(Product::getStock, request.stock());
            changed = true;
        }
        if (request.description() != null) {
            wrapper.set(Product::getDescription, request.description());
            changed = true;
        }
        if (request.image() != null) {
            wrapper.set(Product::getImage, request.image());
            changed = true;
        }
        if (request.status() != null) {
            wrapper.set(Product::getStatus, request.status());
            changed = true;
        }

        if (changed) {
            productMapper.update(null, wrapper);
            cache.evict(RedisKey.cacheProduct(productId));
        }
    }

    @Override
    public void delete(Long productId, Long merchantId) {
        Product product = requireProduct(productId);
        requireOwnedShop(product.getShopId(), merchantId);
        // @TableLogic:逻辑删除
        productMapper.deleteById(productId);
        cache.evict(RedisKey.cacheProduct(productId));
    }

    private Product requireProduct(Long productId) {
        Product product = productMapper.selectById(productId);
        if (product == null) {
            throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND);
        }
        return product;
    }

    private Shop requireShop(Long shopId) {
        Shop shop = shopService.getById(shopId);
        if (shop == null) {
            throw new BusinessException(ErrorCode.SHOP_NOT_FOUND);
        }
        return shop;
    }

    private Shop requireOwnedShop(Long shopId, Long merchantId) {
        Shop shop = requireShop(shopId);
        if (!shop.getMerchantId().equals(merchantId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        return shop;
    }

    private PageResult<ProductBriefResponse> toPage(Page<Product> result) {
        List<ProductBriefResponse> list = result.getRecords().stream()
                .map(ProductConverter::toBrief)
                .toList();
        return PageResult.of(list, result.getCurrent(), result.getSize(), result.getTotal());
    }
}
