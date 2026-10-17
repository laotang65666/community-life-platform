package com.laotang.core.product.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.laotang.common.exception.BusinessException;
import com.laotang.common.exception.ErrorCode;
import com.laotang.common.result.PageResult;
import com.laotang.core.product.entity.Product;
import com.laotang.core.product.mapper.ProductMapper;
import com.laotang.core.shop.entity.Shop;
import com.laotang.core.shop.service.IShopService;
import com.laotang.framework.redis.cache.CacheProperties;
import com.laotang.framework.redis.cache.MultiLevelCache;
import com.laotang.parameter.product.request.CreateProductRequest;
import com.laotang.parameter.product.request.UpdateProductRequest;
import com.laotang.parameter.product.response.ProductBriefResponse;
import com.laotang.parameter.product.response.ProductResponse;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceImplTest {

    @Mock
    private ProductMapper productMapper;
    @Mock
    private IShopService shopService;
    @Mock
    private MultiLevelCache cache;
    @Mock
    private CacheProperties cacheProperties;
    @InjectMocks
    private ProductServiceImpl productService;

    @BeforeAll
    static void initTableInfo() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, Product.class);
    }

    private Shop shop(long merchantId) {
        Shop shop = new Shop();
        shop.setId(10L);
        shop.setMerchantId(merchantId);
        return shop;
    }

    private Product product() {
        Product product = new Product();
        product.setId(100L);
        product.setShopId(10L);
        product.setName("手工水饺");
        product.setPrice(1990L);
        product.setStock(100);
        product.setSold(0);
        product.setStatus("ON_SALE");
        return product;
    }

    private CreateProductRequest createRequest() {
        return new CreateProductRequest("手工水饺", 1990L, 100, "现包现煮", "");
    }

    @Test
    @DisplayName("非店主新增商品 -> 2003")
    void create_notOwner() {
        when(shopService.getById(10L)).thenReturn(shop(2L));

        BusinessException ex = catchThrowableOfType(
                () -> productService.create(10L, 1L, createRequest()), BusinessException.class);

        assertThat(ex).isNotNull();
        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN);
    }

    @Test
    @DisplayName("新增商品成功,默认上架")
    void create_success() {
        when(shopService.getById(10L)).thenReturn(shop(1L));
        when(productMapper.insert(any(Product.class))).thenAnswer(inv -> {
            Product p = inv.getArgument(0);
            p.setId(100L);
            return 1;
        });

        ProductResponse response = productService.create(10L, 1L, createRequest());

        assertThat(response.id()).isEqualTo("100");
        assertThat(response.name()).isEqualTo("手工水饺");
        assertThat(response.price()).isEqualTo(1990L);
        assertThat(response.status()).isEqualTo("ON_SALE");
    }

    @Test
    @DisplayName("商品不存在 -> 5001")
    void getDetail_notFound() {
        when(productMapper.selectById(99L)).thenReturn(null);

        BusinessException ex = catchThrowableOfType(
                () -> productService.getDetail(99L), BusinessException.class);

        assertThat(ex).isNotNull();
        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.PRODUCT_NOT_FOUND);
    }

    @Test
    @DisplayName("非店主修改商品 -> 2003")
    void update_notOwner() {
        when(productMapper.selectById(100L)).thenReturn(product());
        when(shopService.getById(10L)).thenReturn(shop(2L));

        BusinessException ex = catchThrowableOfType(
                () -> productService.update(100L, 1L,
                        new UpdateProductRequest("新名字", null, null, null, null, null)),
                BusinessException.class);

        assertThat(ex).isNotNull();
        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN);
    }

    @Test
    @DisplayName("删除商品执行逻辑删除")
    void delete_success() {
        when(productMapper.selectById(100L)).thenReturn(product());
        when(shopService.getById(10L)).thenReturn(shop(1L));

        productService.delete(100L, 1L);

        verify(productMapper).deleteById(100L);
    }

    @Test
    @DisplayName("店铺在售商品列表")
    void listByShop() {
        Page<Product> page = new Page<>(1, 10);
        page.setRecords(List.of(product()));
        page.setTotal(1);

        when(shopService.getById(10L)).thenReturn(shop(1L));
        when(productMapper.selectPage(any(), any())).thenReturn(page);

        PageResult<ProductBriefResponse> result = productService.listByShop(10L, 1, 10, null);

        assertThat(result.total()).isEqualTo(1);
        assertThat(result.list()).hasSize(1);
        assertThat(result.list().getFirst().name()).isEqualTo("手工水饺");
    }

    @Test
    @DisplayName("非店主查看管理列表 -> 2003")
    void listManage_notOwner() {
        when(shopService.getById(10L)).thenReturn(shop(2L));

        BusinessException ex = catchThrowableOfType(
                () -> productService.listManage(10L, 1L, 1, 10), BusinessException.class);

        assertThat(ex).isNotNull();
        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN);
    }
}
