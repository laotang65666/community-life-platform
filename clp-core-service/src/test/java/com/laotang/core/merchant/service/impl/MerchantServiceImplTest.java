package com.laotang.core.merchant.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.laotang.common.exception.BusinessException;
import com.laotang.common.exception.ErrorCode;
import com.laotang.core.merchant.entity.Merchant;
import com.laotang.core.merchant.mapper.MerchantMapper;
import com.laotang.core.shop.service.IShopService;
import com.laotang.core.user.entity.User;
import com.laotang.core.user.service.IUserService;
import com.laotang.parameter.merchant.request.ApplyMerchantRequest;
import com.laotang.parameter.merchant.response.MerchantResponse;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MerchantServiceImplTest {

    @Mock
    private MerchantMapper merchantMapper;
    @Mock
    private IUserService userService;
    @Mock
    private IShopService shopService;
    @InjectMocks
    private MerchantServiceImpl merchantService;

    @BeforeAll
    static void initTableInfo() {
        // 纯单元测试没有走 MyBatis-Plus 启动流程,手动初始化实体元数据,
        // 否则 LambdaUpdateWrapper.set(Merchant::getXxx, ...) 会报找不到 lambda cache。
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, Merchant.class);
    }

    private ApplyMerchantRequest request() {
        return new ApplyMerchantRequest(
                "社区小厨", "91110000TEST", "3 号楼底商", "张三", "13800138000");
    }

    @Test
    @DisplayName("首次申请 -> 插入 PENDING 记录")
    void apply_newApplication() {
        when(merchantMapper.selectOne(any())).thenReturn(null);
        when(merchantMapper.insert(any(Merchant.class))).thenAnswer(inv -> {
            Merchant merchant = inv.getArgument(0);
            merchant.setId(100L);
            return 1;
        });

        MerchantResponse response = merchantService.apply(1L, request());

        assertThat(response.id()).isEqualTo("100");
        assertThat(response.shopName()).isEqualTo("社区小厨");
        assertThat(response.auditStatus()).isEqualTo("PENDING");
    }

    @Test
    @DisplayName("已有 PENDING 申请 -> 4002")
    void apply_pendingExists() {
        Merchant existing = new Merchant();
        existing.setAuditStatus("PENDING");
        when(merchantMapper.selectOne(any())).thenReturn(existing);

        BusinessException ex = catchThrowableOfType(
                () -> merchantService.apply(1L, request()), BusinessException.class);

        assertThat(ex).isNotNull();
        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.MERCHANT_AUDITING);
    }

    @Test
    @DisplayName("已经是商户 -> 4005")
    void apply_alreadyApproved() {
        Merchant existing = new Merchant();
        existing.setAuditStatus("APPROVED");
        when(merchantMapper.selectOne(any())).thenReturn(existing);

        BusinessException ex = catchThrowableOfType(
                () -> merchantService.apply(1L, request()), BusinessException.class);

        assertThat(ex).isNotNull();
        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.MERCHANT_ALREADY_APPROVED);
    }

    @Test
    @DisplayName("审核通过 -> 创建店铺 + 升级角色")
    void approve_success() {
        Merchant merchant = new Merchant();
        merchant.setId(10L);
        merchant.setUserId(1L);
        merchant.setAuditStatus("PENDING");
        merchant.setShopName("社区小厨");

        when(merchantMapper.selectById(10L)).thenReturn(merchant);
        when(userService.getById(1L)).thenReturn(new User());

        merchantService.approve(10L, 99L, 5L);

        verify(shopService).createForMerchant(merchant, 5L);
        verify(userService).updateRole(1L, "MERCHANT");
        verify(merchantMapper).update(isNull(), any());
    }

    @Test
    @DisplayName("非待审核状态不能通过 -> 4002")
    void approve_notPending() {
        Merchant merchant = new Merchant();
        merchant.setAuditStatus("APPROVED");
        when(merchantMapper.selectById(10L)).thenReturn(merchant);

        BusinessException ex = catchThrowableOfType(
                () -> merchantService.approve(10L, 99L, 5L), BusinessException.class);

        assertThat(ex).isNotNull();
        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.MERCHANT_AUDITING);
    }

    @Test
    @DisplayName("驳回 -> 更新状态与原因")
    void reject_success() {
        Merchant merchant = new Merchant();
        merchant.setId(10L);
        merchant.setAuditStatus("PENDING");
        when(merchantMapper.selectById(10L)).thenReturn(merchant);

        assertThatCode(() -> merchantService.reject(10L, 99L, "执照不清晰"))
                .doesNotThrowAnyException();

        verify(merchantMapper).update(isNull(), any());
    }
}
