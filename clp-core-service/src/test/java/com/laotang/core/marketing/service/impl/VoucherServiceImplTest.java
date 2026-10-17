package com.laotang.core.marketing.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.laotang.common.exception.BusinessException;
import com.laotang.common.exception.ErrorCode;
import com.laotang.core.marketing.entity.SeckillVoucher;
import com.laotang.core.marketing.entity.Voucher;
import com.laotang.core.marketing.mapper.SeckillVoucherMapper;
import com.laotang.core.marketing.mapper.VoucherMapper;
import com.laotang.core.shop.entity.Shop;
import com.laotang.core.shop.service.IShopService;
import com.laotang.parameter.marketing.request.CreateVoucherRequest;
import com.laotang.parameter.marketing.response.VoucherResponse;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VoucherServiceImplTest {

    @Mock
    private VoucherMapper voucherMapper;
    @Mock
    private SeckillVoucherMapper seckillVoucherMapper;
    @Mock
    private IShopService shopService;
    @Mock
    private StringRedisTemplate redis;
    @Mock
    private ValueOperations<String, String> valueOps;
    @InjectMocks
    private VoucherServiceImpl voucherService;

    @BeforeAll
    static void initTableInfo() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, Voucher.class);
        TableInfoHelper.initTableInfo(assistant, SeckillVoucher.class);
    }

    @BeforeEach
    void setUp() {
        lenient().when(redis.opsForValue()).thenReturn(valueOps);
    }

    private Shop shop(long merchantId) {
        Shop shop = new Shop();
        shop.setId(10L);
        shop.setMerchantId(merchantId);
        return shop;
    }

    private CreateVoucherRequest request(LocalDateTime begin, LocalDateTime end) {
        return new CreateVoucherRequest(
                10L, "满减券", "测试", "规则",
                1000L, 2000L, 100, 1, begin, end);
    }

    @Test
    @DisplayName("非店主不能创建优惠券 -> 2003")
    void create_notOwner() {
        when(shopService.getById(10L)).thenReturn(shop(2L));

        BusinessException ex = catchThrowableOfType(
                () -> voucherService.create(1L,
                        request(LocalDateTime.now(), LocalDateTime.now().plusHours(1))),
                BusinessException.class);

        assertThat(ex).isNotNull();
        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN);
    }

    @Test
    @DisplayName("结束时间早于开始时间 -> 1001")
    void create_invalidTime() {
        BusinessException ex = catchThrowableOfType(
                () -> voucherService.create(1L,
                        request(LocalDateTime.now(), LocalDateTime.now().minusHours(1))),
                BusinessException.class);

        assertThat(ex).isNotNull();
        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.PARAM_INVALID);
    }

    @Test
    @DisplayName("创建成功并预热 Redis 库存")
    void create_success() {
        when(shopService.getById(10L)).thenReturn(shop(1L));
        when(voucherMapper.insert(any(Voucher.class))).thenAnswer(inv -> {
            Voucher voucher = inv.getArgument(0);
            voucher.setId(100L);
            return 1;
        });
        when(seckillVoucherMapper.insert(any(SeckillVoucher.class))).thenReturn(1);

        VoucherResponse response = voucherService.create(1L,
                request(LocalDateTime.now(), LocalDateTime.now().plusHours(1)));

        assertThat(response.id()).isEqualTo("100");
        assertThat(response.stock()).isEqualTo(100);
        assertThat(response.type()).isEqualTo("SECKILL");
        verify(valueOps).set("clp:seckill:stock:100", "100");
    }
}
