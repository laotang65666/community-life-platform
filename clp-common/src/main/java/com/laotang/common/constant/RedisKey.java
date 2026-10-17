package com.laotang.common.constant;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Redis Key 统一拼接。业务代码禁止手写 key 字符串。
 */
public final class RedisKey {

    private static final String PREFIX = "clp:";

    public static final String AUTH_SMS = PREFIX + "auth:sms:";
    public static final String AUTH_SMS_LIMIT = PREFIX + "auth:sms:limit:";
    public static final String AUTH_SMS_FAIL = PREFIX + "auth:sms:fail:";
    public static final String AUTH_LOCK = PREFIX + "auth:lock:";
    public static final String AUTH_REFRESH = PREFIX + "auth:refresh:";

    public static final String SECKILL_STOCK = PREFIX + "seckill:stock:";
    public static final String SECKILL_USERS = PREFIX + "seckill:users:";
    public static final String SECKILL_TOKEN = PREFIX + "seckill:token:";
    public static final String SECKILL_TOKEN_LIMIT = PREFIX + "seckill:token-limit:";
    public static final String SECKILL_VOUCHER = PREFIX + "seckill:voucher:";
    public static final String SECKILL_IDEM = PREFIX + "idem:seckill:";

    public static final String BLOG_LIKE = PREFIX + "like:blog:";
    public static final String USER_FEED = PREFIX + "feed:user:";
    public static final String USER_OUTBOX = PREFIX + "outbox:user:";
    public static final String USER_FOLLOWERS = PREFIX + "user:followers:";
    public static final String SIGN = PREFIX + "sign:";

    /**
     * WebSocket 跨节点广播的 Redis Pub/Sub 频道。
     */
    public static final String NOTIFY_CHANNEL = PREFIX + "notify:channel";

    public static final String AI_CHAT_RATE = PREFIX + "ai:chat:rate:";
    public static final String AI_CONVERSATION_LOCK = PREFIX + "ai:conversation:lock:";
    public static final String AI_QUERY_EMBEDDING = PREFIX + "ai:embedding:query:";

    private RedisKey() {
    }

    public static String sms(String phone) {
        return AUTH_SMS + phone;
    }

    public static String smsLimit(String phone) {
        return AUTH_SMS_LIMIT + phone;
    }

    public static String smsFail(String phone) {
        return AUTH_SMS_FAIL + phone;
    }

    public static String loginLock(String phone) {
        return AUTH_LOCK + phone;
    }

    public static String refresh(Long userId, String jti) {
        return AUTH_REFRESH + userId + ":" + jti;
    }

    public static String seckillStock(Long voucherId) {
        return SECKILL_STOCK + voucherId;
    }

    public static String seckillUsers(Long voucherId) {
        return SECKILL_USERS + voucherId;
    }

    public static String seckillToken(String token) {
        return SECKILL_TOKEN + token;
    }

    public static String seckillTokenLimit(Long userId, Long voucherId) {
        return SECKILL_TOKEN_LIMIT + userId + ":" + voucherId;
    }

    public static String seckillVoucher(Long voucherId) {
        return SECKILL_VOUCHER + voucherId;
    }

    public static String seckillIdempotency(Long userId, String idemKey) {
        return SECKILL_IDEM + userId + ":" + idemKey;
    }

    public static String blogLike(Long blogId) {
        return BLOG_LIKE + blogId;
    }

    public static String userFeed(Long userId) {
        return USER_FEED + userId;
    }

    public static String userOutbox(Long userId) {
        return USER_OUTBOX + userId;
    }

    public static String userFollowers(Long userId) {
        return USER_FOLLOWERS + userId;
    }

    public static String sign(Long userId, LocalDate date) {
        return SIGN + userId + ":" + date.format(DateTimeFormatter.ofPattern("yyyyMM"));
    }

    public static String aiChatRate(Long userId) {
        return AI_CHAT_RATE + userId;
    }

    public static String aiConversationLock(Long conversationId) {
        return AI_CONVERSATION_LOCK + conversationId;
    }

    public static String aiQueryEmbedding(String contentHash) {
        return AI_QUERY_EMBEDDING + contentHash;
    }
}
