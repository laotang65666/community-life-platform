package com.laotang.core.auth.support;

/**
 * 短信发送抽象。开发环境打日志,生产替换为真实服务商。
 */
public interface SmsSender {

    void send(String phone, String code);
}
