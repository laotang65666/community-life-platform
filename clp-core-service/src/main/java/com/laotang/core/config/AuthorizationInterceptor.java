package com.laotang.core.config;

import com.laotang.common.annotation.RequireRole;
import com.laotang.common.context.LoginUser;
import com.laotang.common.context.UserContext;
import com.laotang.common.exception.BusinessException;
import com.laotang.common.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 根据 @RequireRole 校验登录状态与角色。
 *
 * <p>没有注解 = 公开接口;有注解 = 必须登录且角色等级足够。</p>
 */
@Component
public class AuthorizationInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) {
        if (!(handler instanceof HandlerMethod method)) {
            return true;
        }

        RequireRole required = method.getMethodAnnotation(RequireRole.class);
        if (required == null) {
            required = method.getBeanType().getAnnotation(RequireRole.class);
        }
        if (required == null) {
            return true;
        }

        LoginUser user = UserContext.get();
        if (user == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        if (!user.role().hasAtLeast(required.value())) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        return true;
    }
}
