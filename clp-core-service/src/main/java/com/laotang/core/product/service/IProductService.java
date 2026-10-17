package com.laotang.core.product.service;

import com.laotang.common.result.PageResult;
import com.laotang.parameter.product.request.CreateProductRequest;
import com.laotang.parameter.product.request.UpdateProductRequest;
import com.laotang.parameter.product.response.ProductBriefResponse;
import com.laotang.parameter.product.response.ProductResponse;

/**
 * 商品服务。
 */
public interface IProductService {

    /**
     * 店铺在售商品(公开)。
     */
    PageResult<ProductBriefResponse> listByShop(Long shopId, long page, long size, String keyword);

    /**
     * 店铺全部商品(含下架),仅店主可用。
     */
    PageResult<ProductBriefResponse> listManage(Long shopId, Long merchantId, long page, long size);

    ProductResponse getDetail(Long productId);

    ProductResponse create(Long shopId, Long merchantId, CreateProductRequest request);

    void update(Long productId, Long merchantId, UpdateProductRequest request);

    void delete(Long productId, Long merchantId);
}
