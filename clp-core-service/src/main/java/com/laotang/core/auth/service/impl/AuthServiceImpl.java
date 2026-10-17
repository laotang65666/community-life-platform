package com.laotang.core.auth.service.impl;

import com.laotang.common.constant.RedisKey;
import com.laotang.common.enums.RoleEnum;
import com.laotang.common.exception.BusinessException;
import com.laotang.common.exception.ErrorCode;
import com.laotang.core.auth.jwt.JwtProperties;
import com.laotang.core.auth.jwt.JwtTokenProvider;
import com.laotang.core.auth.service.AuthService;
import com.laotang.core.auth.support.SmsSender;
import com.laotang.core.user.entity.User;
import com.laotang.core.user.service.IUserService;
import com.laotang.parameter.auth.response.LoginResponse;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.UUID;

/**
 * 验证码登录 / 刷新 / 登出。
 */
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final Duration CODE_TTL = Duration.ofMinutes(5);
    private static final Duration SEND_INTERVAL = Duration.ofSeconds(60);
    private static final Duration FAIL_TTL = Duration.ofMinutes(10);
    private static final Duration LOCK_TTL = Duration.ofMinutes(10);
    private static final int MAX_FAIL_COUNT = 5;
    private static final int CODE_LENGTH = 6;
    private static final String STATUS_ACTIVE = "ACTIVE";

    private final StringRedisTemplate redis;
    private final IUserService userService;
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtProperties jwtProperties;
    private final SmsSender smsSender;

    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    public void sendSmsCode(String phone) {
        if (Boolean.TRUE.equals(redis.hasKey(RedisKey.loginLock(phone)))) {
            throw new BusinessException(ErrorCode.LOGIN_LOCKED);
        }

        Boolean acquired = redis.opsForValue()
                .setIfAbsent(RedisKey.smsLimit(phone), "1", SEND_INTERVAL);
        if (!Boolean.TRUE.equals(acquired)) {
            throw new BusinessException(ErrorCode.SMS_TOO_FREQUENT);
        }

        String code = generateCode();
        redis.opsForValue().set(RedisKey.sms(phone), code, CODE_TTL);
        smsSender.send(phone, code);
    }

    @Override
    public LoginResponse login(String phone, String code) {
        if (Boolean.TRUE.equals(redis.hasKey(RedisKey.loginLock(phone)))) {
            throw new BusinessException(ErrorCode.LOGIN_LOCKED);
        }

        String cachedCode = redis.opsForValue().get(RedisKey.sms(phone));
        if (cachedCode == null || !cachedCode.equals(code)) {
            recordFailure(phone);
            throw new BusinessException(ErrorCode.SMS_CODE_INVALID);
        }

        redis.delete(RedisKey.sms(phone));
        redis.delete(RedisKey.smsFail(phone));

        User user = userService.findByPhone(phone);
        if (user == null) {
            user = userService.createByPhone(phone);
        }
        if (!STATUS_ACTIVE.equals(user.getStatus())) {
            throw new BusinessException(ErrorCode.ACCOUNT_DISABLED);
        }

        LoginResponse response = issueTokens(user.getId(), RoleEnum.of(user.getRole()));
        userService.updateLastLogin(user.getId());
        return response;
    }

    @Override
    public LoginResponse refresh(String refreshToken) {
        Claims claims = jwtTokenProvider.parse(refreshToken);
        if (!jwtTokenProvider.isRefreshToken(claims)) {
            throw new BusinessException(ErrorCode.TOKEN_INVALID);
        }

        Long userId = jwtTokenProvider.getUserId(claims);
        String jti = jwtTokenProvider.getJti(claims);
        String refreshKey = RedisKey.refresh(userId, jti);
        if (!Boolean.TRUE.equals(redis.hasKey(refreshKey))) {
            throw new BusinessException(ErrorCode.TOKEN_INVALID);
        }

        User user = userService.getById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.TOKEN_INVALID);
        }
        if (!STATUS_ACTIVE.equals(user.getStatus())) {
            throw new BusinessException(ErrorCode.ACCOUNT_DISABLED);
        }

        // 轮换:旧 refreshToken 立即失效
        redis.delete(refreshKey);
        return issueTokens(userId, RoleEnum.of(user.getRole()));
    }

    @Override
    public void logout(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return;
        }
        try {
            Claims claims = jwtTokenProvider.parse(refreshToken);
            if (!jwtTokenProvider.isRefreshToken(claims)) {
                return;
            }
            redis.delete(RedisKey.refresh(jwtTokenProvider.getUserId(claims), jwtTokenProvider.getJti(claims)));
        } catch (BusinessException e) {
            // 登出幂等:token 已失效也视为成功
        }
    }

    private LoginResponse issueTokens(Long userId, RoleEnum role) {
        String accessToken = jwtTokenProvider.createAccessToken(userId, role);
        String jti = UUID.randomUUID().toString();
        String refreshToken = jwtTokenProvider.createRefreshToken(userId, jti);
        redis.opsForValue().set(RedisKey.refresh(userId, jti), "1", jwtProperties.refreshTokenExpire());
        return new LoginResponse(
                accessToken,
                refreshToken,
                jwtProperties.accessTokenExpire().toSeconds(),
                role.name());
    }

    private void recordFailure(String phone) {
        String failKey = RedisKey.smsFail(phone);
        Long failCount = redis.opsForValue().increment(failKey);
        redis.expire(failKey, FAIL_TTL);
        if (failCount != null && failCount >= MAX_FAIL_COUNT) {
            redis.opsForValue().set(RedisKey.loginLock(phone), "1", LOCK_TTL);
        }
    }

    private String generateCode() {
        int bound = (int) Math.pow(10, CODE_LENGTH);
        return String.format("%0" + CODE_LENGTH + "d", secureRandom.nextInt(bound));
    }
}
