#!/usr/bin/env bash
# 全链路冒烟测试
#
# 用法:
#   APP_LOG=/path/to/app.log bash loadtest/smoke-test.sh
#
# 可选环境变量:
#   BASE_URL        服务地址,默认 http://localhost:8084
#   PHONE           普通用户手机号,默认 13700000012
#   APP_LOG         应用日志路径(用于读取验证码);与 SMS_CODE 二选一
#   SMS_CODE        已知验证码,设置后不再读取日志
#   RUN_AI=1        额外验证 AI 会话与问答(会消耗模型 token)
#   MERCHANT_PHONE  商户手机号,设置后额外验证接单/完成
#   MERCHANT_SMS_CODE 商户验证码,不填则从 APP_LOG 读取
#
# 退出码:全部通过返回 0,否则返回 1。

set -uo pipefail

BASE_URL=${BASE_URL:-http://localhost:8084}
PHONE=${PHONE:-13700000012}
APP_LOG=${APP_LOG:-}
SMS_CODE=${SMS_CODE:-}
RUN_AI=${RUN_AI:-0}
MERCHANT_PHONE=${MERCHANT_PHONE:-}
MERCHANT_SMS_CODE=${MERCHANT_SMS_CODE:-}
PY=${PY:-python}

PASS=0
FAIL=0

pass() { PASS=$((PASS + 1)); echo "PASS  $1"; }
fail() {
    FAIL=$((FAIL + 1))
    echo "FAIL  $1"
    [ -n "${2:-}" ] && echo "      ${2}"
}

code_of() {
    printf '%s' "$1" | "$PY" -c 'import sys,json; print(json.load(sys.stdin).get("code","parse_error"))' 2>/dev/null || echo parse_error
}

value_of() {
    printf '%s' "$1" | "$PY" -c "import sys,json; print(json.load(sys.stdin)$2)" 2>/dev/null
}

check_code() {
    local name=$1 body=$2 expected=${3:-0}
    local code
    code=$(code_of "$body")
    if [ "$code" = "$expected" ]; then
        pass "$name (code=$code)"
    else
        fail "$name (code=$code, expected=$expected)" "$(printf '%s' "$body" | head -c 300)"
    fi
}

sms_code_for() {
    local phone=$1
    if [ -z "$APP_LOG" ]; then
        echo ""
        return
    fi
    grep "\[SMS\] phone=$phone" "$APP_LOG" 2>/dev/null | tail -1 | sed 's/.*code=//'
}

login() {
    local phone=$1 code=$2
    curl -s -X POST "$BASE_URL/api/v1/auth/login" \
        -H "Content-Type: application/json" \
        -d "{\"phone\":\"$phone\",\"code\":\"$code\"}"
}

echo "== 冒烟测试: $BASE_URL =="

# ---------- 0. 健康检查 ----------
HEALTH=$(curl -s "$BASE_URL/actuator/health")
if [ "$(value_of "$HEALTH" '["status"]')" = "UP" ]; then
    pass "健康检查"
else
    fail "健康检查" "$HEALTH"
    echo "服务未就绪,终止。"
    exit 1
fi

# ---------- 1. 登录 ----------
if [ -z "$SMS_CODE" ]; then
    if [ -z "$APP_LOG" ]; then
        echo "需要 APP_LOG 或 SMS_CODE 才能获取验证码。"
        exit 1
    fi
    curl -s -X POST "$BASE_URL/api/v1/auth/sms-code" \
        -H "Content-Type: application/json" \
        -d "{\"phone\":\"$PHONE\"}" > /dev/null
    sleep 1
    SMS_CODE=$(sms_code_for "$PHONE")
fi

LOGIN=$(login "$PHONE" "$SMS_CODE")
check_code "用户登录" "$LOGIN"
TOKEN=$(value_of "$LOGIN" '["data"]["accessToken"]')
if [ -z "$TOKEN" ] || [ "$TOKEN" = "None" ]; then
    echo "登录失败,终止。"
    exit 1
fi
AUTH="Authorization: Bearer $TOKEN"

# ---------- 2. 用户资料 ----------
ME=$(curl -s "$BASE_URL/api/v1/users/me" -H "$AUTH")
check_code "查询个人资料" "$ME"

# ---------- 3. 店铺与商品 ----------
SHOPS=$(curl -s "$BASE_URL/api/v1/shops?page=1&size=1")
check_code "店铺列表" "$SHOPS"
SHOP_ID=$(value_of "$SHOPS" '["data"]["list"][0]["id"]')

if [ -z "$SHOP_ID" ] || [ "$SHOP_ID" = "None" ]; then
    fail "取店铺 ID" "列表为空,请先准备测试店铺"
    echo "PASS=$PASS FAIL=$FAIL"
    exit 1
fi

SHOP=$(curl -s "$BASE_URL/api/v1/shops/$SHOP_ID")
check_code "店铺详情(缓存)" "$SHOP"

PRODUCTS=$(curl -s "$BASE_URL/api/v1/shops/$SHOP_ID/products?page=1&size=1")
check_code "店铺商品列表" "$PRODUCTS"
PRODUCT_ID=$(value_of "$PRODUCTS" '["data"]["list"][0]["id"]')

if [ -z "$PRODUCT_ID" ] || [ "$PRODUCT_ID" = "None" ]; then
    fail "取商品 ID" "店铺没有在售商品"
    echo "PASS=$PASS FAIL=$FAIL"
    exit 1
fi

PRODUCT=$(curl -s "$BASE_URL/api/v1/products/$PRODUCT_ID")
check_code "商品详情(缓存)" "$PRODUCT"

# ---------- 4. 下单与支付 ----------
IDEM_KEY="smoke-$(date +%s)-$RANDOM"
ORDER_BODY="{\"shopId\":$SHOP_ID,\"items\":[{\"productId\":$PRODUCT_ID,\"quantity\":1}],\"remark\":\"smoke test\"}"
ORDER=$(curl -s -X POST "$BASE_URL/api/v1/orders" \
    -H "Content-Type: application/json" \
    -H "$AUTH" \
    -H "Idempotency-Key: $IDEM_KEY" \
    --data-binary "$ORDER_BODY")
check_code "创建订单" "$ORDER"
ORDER_ID=$(value_of "$ORDER" '["data"]["id"]')

if [ -n "$ORDER_ID" ] && [ "$ORDER_ID" != "None" ]; then
    PAY=$(curl -s -X POST "$BASE_URL/api/v1/orders/$ORDER_ID/pay" -H "$AUTH")
    check_code "支付订单" "$PAY"

    DETAIL=$(curl -s "$BASE_URL/api/v1/orders/$ORDER_ID" -H "$AUTH")
    check_code "订单详情" "$DETAIL"

    # 重复提交同一个幂等键,应返回同一笔订单
    ORDER_AGAIN=$(curl -s -X POST "$BASE_URL/api/v1/orders" \
        -H "Content-Type: application/json" \
        -H "$AUTH" \
        -H "Idempotency-Key: $IDEM_KEY" \
        --data-binary "$ORDER_BODY")
    ORDER_ID_AGAIN=$(value_of "$ORDER_AGAIN" '["data"]["id"]')
    if [ "$ORDER_ID_AGAIN" = "$ORDER_ID" ]; then
        pass "下单幂等"
    else
        fail "下单幂等" "第二次返回 $ORDER_ID_AGAIN,期望 $ORDER_ID"
    fi
fi

# ---------- 5. 通知 ----------
NOTIFY=$(curl -s "$BASE_URL/api/v1/notifications?page=1&size=5" -H "$AUTH")
check_code "通知列表" "$NOTIFY"

# ---------- 6. 可选的商户履约 ----------
if [ -n "$MERCHANT_PHONE" ] && [ -n "$ORDER_ID" ]; then
    if [ -z "$MERCHANT_SMS_CODE" ]; then
        curl -s -X POST "$BASE_URL/api/v1/auth/sms-code" \
            -H "Content-Type: application/json" \
            -d "{\"phone\":\"$MERCHANT_PHONE\"}" > /dev/null
        sleep 1
        MERCHANT_SMS_CODE=$(sms_code_for "$MERCHANT_PHONE")
    fi
    MERCHANT_LOGIN=$(login "$MERCHANT_PHONE" "$MERCHANT_SMS_CODE")
    check_code "商户登录" "$MERCHANT_LOGIN"
    MERCHANT_TOKEN=$(value_of "$MERCHANT_LOGIN" '["data"]["accessToken"]')
    MAUTH="Authorization: Bearer $MERCHANT_TOKEN"

    ACCEPT=$(curl -s -X POST "$BASE_URL/api/v1/orders/$ORDER_ID/accept" -H "$MAUTH")
    check_code "商户接单" "$ACCEPT"

    COMPLETE=$(curl -s -X POST "$BASE_URL/api/v1/orders/$ORDER_ID/complete" -H "$MAUTH")
    check_code "商户完成" "$COMPLETE"
fi

# ---------- 7. 可选的 AI 问答 ----------
if [ "$RUN_AI" = "1" ]; then
    CONV=$(curl -s -X POST "$BASE_URL/api/v1/ai/conversations" \
        -H "Content-Type: application/json" \
        -H "$AUTH" \
        -d '{"scope":"USER"}')
    check_code "创建 AI 会话" "$CONV"
    CONV_ID=$(value_of "$CONV" '["data"]["id"]')

    if [ -n "$CONV_ID" ] && [ "$CONV_ID" != "None" ]; then
        CHAT=$(curl -s -X POST "$BASE_URL/api/v1/ai/conversations/$CONV_ID/messages" \
            -H "Content-Type: application/json" \
            -H "$AUTH" \
            --data-binary '{"content":"订单多久不支付会自动关闭?"}')
        check_code "AI 问答" "$CHAT"
        DEGRADED=$(value_of "$CHAT" '["data"]["degraded"]')
        SOURCES=$(value_of "$CHAT" '["data"]["sources"]')
        echo "      AI degraded=$DEGRADED sources=$SOURCES"
    fi
fi

# ---------- 汇总 ----------
echo "== 结果: PASS=$PASS FAIL=$FAIL =="
if [ "$FAIL" -gt 0 ]; then
    exit 1
fi
