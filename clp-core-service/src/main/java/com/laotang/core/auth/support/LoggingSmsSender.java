package com.laotang.core.auth.support;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 开发环境短信发送:把验证码打到日志,方便本地测试。
 */
@Component
public class LoggingSmsSender implements SmsSender {

    private static final Logger log = LoggerFactory.getLogger(LoggingSmsSender.class);

    @Override
    public void send(String phone, String code) {
        log.info("[SMS] phone={} code={}", phone, code);
    }
}
