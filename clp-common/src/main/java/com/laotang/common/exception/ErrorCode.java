package com.laotang.common.exception;

/**
 * 业务错误码。
 *
 * <p>编码规则见 docs/02-接口规范.md:</p>
 * <pre>
 * 0      成功
 * 1xxx   通用 / 系统
 * 2xxx   认证与权限
 * 3xxx   用户
 * 4xxx   商户与店铺
 * 5xxx   商品
 * 6xxx   订单
 * 7xxx   营销与秒杀
 * 8xxx   内容社区
 * 9xxx   签到 / 积分 / 通知
 * 10xxx  AI
 * 11xxx  管理后台
 * </pre>
 */
public enum ErrorCode {

    // ---------- 通用 ----------
    SUCCESS(0, "ok", 200),
    SYSTEM_ERROR(1000, "系统繁忙,请稍后重试", 500),
    PARAM_INVALID(1001, "参数校验失败", 400),
    NOT_FOUND(1002, "资源不存在", 404),
    TOO_MANY_REQUESTS(1003, "请求过于频繁", 429),
    SERVICE_UNAVAILABLE(1004, "依赖服务不可用", 503),

    // ---------- 认证与权限 ----------
    UNAUTHORIZED(2001, "未登录", 401),
    TOKEN_INVALID(2002, "token 无效或已过期", 401),
    FORBIDDEN(2003, "无权限访问", 403),
    ACCOUNT_DISABLED(2004, "账号已被禁用", 403),

    // ---------- 用户 ----------
    PHONE_REGISTERED(3001, "手机号已注册", 409),
    SMS_CODE_INVALID(3002, "验证码错误或已过期", 422),
    SMS_TOO_FREQUENT(3003, "验证码发送过于频繁", 429),
    LOGIN_LOCKED(3004, "登录失败次数过多,请 10 分钟后再试", 403),
    CANNOT_FOLLOW_SELF(3005, "不能关注自己", 422),

    // ---------- 商户与店铺 ----------
    MERCHANT_NOT_FOUND(4001, "商户信息不存在", 404),
    MERCHANT_AUDITING(4002, "已有审核中的入驻申请", 409),
    MERCHANT_NOT_APPROVED(4003, "商户审核未通过", 403),
    SHOP_CLOSED(4004, "店铺已关闭", 403),
    MERCHANT_ALREADY_APPROVED(4005, "您已经是商户", 409),
    SHOP_NOT_FOUND(4006, "店铺不存在", 404);

    private final int code;
    private final String message;
    private final int httpStatus;

    ErrorCode(int code, String message, int httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public int getHttpStatus() {
        return httpStatus;
    }
}
