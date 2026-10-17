package com.laotang.core.ai.rag;

import com.laotang.common.exception.BusinessException;
import com.laotang.common.exception.ErrorCode;

/**
 * 知识分片的适用范围。
 */
public enum KnowledgeScope {

    USER,

    MERCHANT,

    /**
     * 两端都可见的平台通用知识。
     */
    ALL;

    public static KnowledgeScope from(String value) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "scope 不能为空");
        }
        for (KnowledgeScope scope : values()) {
            if (scope.name().equalsIgnoreCase(value.trim())) {
                return scope;
            }
        }
        throw new BusinessException(ErrorCode.PARAM_INVALID, "未知知识范围: " + value);
    }
}
