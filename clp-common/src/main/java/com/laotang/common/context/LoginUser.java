package com.laotang.common.context;

import com.laotang.common.enums.RoleEnum;

/**
 * 当前登录用户快照。
 */
public record LoginUser(Long userId, String phone, RoleEnum role) {
}
