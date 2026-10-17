package com.laotang.core.auth.service;

import com.laotang.parameter.auth.response.LoginResponse;

/**
 * 认证服务。
 */
public interface AuthService {

    void sendSmsCode(String phone);

    LoginResponse login(String phone, String code);

    LoginResponse refresh(String refreshToken);

    void logout(String refreshToken);
}
