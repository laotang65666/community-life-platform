package com.laotang.core.ai.enums;

import com.laotang.common.enums.RoleEnum;
import com.laotang.common.exception.BusinessException;
import com.laotang.common.exception.ErrorCode;

/**
 * AI 助手的使用范围。
 *
 * <p>两个范围共用一套对话存储,但系统提示词、可调用工具与知识库过滤条件不同。</p>
 */
public enum AiScope {

    /**
     * 用户端客服:下单、优惠券、秒杀、积分、售后。
     */
    USER,

    /**
     * 商户端助手:商品、订单履约、店铺经营、入驻审核。
     */
    MERCHANT;

    public static AiScope from(String value) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "scope 不能为空");
        }
        for (AiScope scope : values()) {
            if (scope.name().equalsIgnoreCase(value.trim())) {
                return scope;
            }
        }
        throw new BusinessException(ErrorCode.PARAM_INVALID, "未知 scope: " + value);
    }

    /**
     * 当前登录角色能否使用该助手。
     */
    public boolean availableTo(RoleEnum role) {
        if (role == null) {
            return false;
        }
        return this != MERCHANT || role.hasAtLeast(RoleEnum.MERCHANT);
    }
}
