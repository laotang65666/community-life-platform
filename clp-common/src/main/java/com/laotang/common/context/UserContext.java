package com.laotang.common.context;

import com.laotang.common.enums.RoleEnum;

/**
 * 当前请求的用户上下文。
 *
 * <p>由拦截器在请求进入时写入,请求结束时必须清理,避免线程池复用导致串号。</p>
 */
public final class UserContext {

    private static final ThreadLocal<LoginUser> HOLDER = new ThreadLocal<>();

    private UserContext() {
    }

    public static void set(LoginUser user) {
        HOLDER.set(user);
    }

    public static LoginUser get() {
        return HOLDER.get();
    }

    public static Long getUserId() {
        LoginUser user = HOLDER.get();
        return user == null ? null : user.userId();
    }

    public static RoleEnum getRole() {
        LoginUser user = HOLDER.get();
        return user == null ? null : user.role();
    }

    public static void clear() {
        HOLDER.remove();
    }
}
