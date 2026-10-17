package com.laotang.common.annotation;

import com.laotang.common.enums.RoleEnum;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记接口所需的最低角色。
 *
 * <p>不标注 = 公开接口;标注后必须登录且角色等级足够。</p>
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequireRole {

    RoleEnum value() default RoleEnum.USER;
}
