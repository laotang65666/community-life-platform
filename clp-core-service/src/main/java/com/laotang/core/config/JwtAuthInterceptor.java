package com.laotang.core.config;

import com.laotang.common.context.LoginUser;
import com.laotang.common.context.UserContext;
import com.laotang.common.exception.BusinessException;
import com.laotang.common.exception.ErrorCode;
import com.laotang.core.auth.jwt.JwtTokenProvider;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 解析 Authorization 中的 accessToken,写入 UserContext。
 *
 * <p>没有 token 时直接放行,由 {@link AuthorizationInterceptor} 判断接口是否需要登录。</p>
 */
@Component
@RequiredArgsConstructor
public class JwtAuthInterceptor implements HandlerInterceptor {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) {
        String header = request.getHeader("Authorization");
        if (!StringUtils.hasText(header) || !header.startsWith(BEARER_PREFIX)) {
            return true;
        }

        String token = header.substring(BEARER_PREFIX.length());
        Claims claims = jwtTokenProvider.parse(token);
        if (!jwtTokenProvider.isAccessToken(claims)) {
            throw new BusinessException(ErrorCode.TOKEN_INVALID);
        }

        UserContext.set(new LoginUser(
                jwtTokenProvider.getUserId(claims),
                null,
                jwtTokenProvider.getRole(claims)));
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request,
                                HttpServletResponse response,
                                Object handler,
                                Exception ex) {
        UserContext.clear();
    }
}
