-- ============================================================
-- 秒杀压测结果验收
-- 把 @voucher_id 换成实际券 ID
-- ============================================================

SET @voucher_id = 0;

-- 1. 订单数(应等于券初始库存)
SELECT COUNT(*) AS order_count
FROM clp.tb_voucher_order
WHERE voucher_id = @voucher_id;

-- 2. 去重用户数(应等于订单数,不能有重复)
SELECT COUNT(*) AS total,
       COUNT(DISTINCT user_id) AS distinct_users
FROM clp.tb_voucher_order
WHERE voucher_id = @voucher_id;

-- 3. DB 秒杀库存(应为 0)
SELECT stock, initial_stock
FROM clp.tb_seckill_voucher
WHERE voucher_id = @voucher_id;

-- 4. Outbox 状态(成功订单应全部 ACKED)
SELECT status, COUNT(*) AS cnt
FROM clp.tb_outbox
GROUP BY status;

-- 5. 是否存在同一用户多单(应为空)
SELECT user_id, COUNT(*) AS cnt
FROM clp.tb_voucher_order
WHERE voucher_id = @voucher_id
GROUP BY user_id
HAVING COUNT(*) > 1;

-- 6. 是否存在库存负数(应为空)
SELECT voucher_id, stock
FROM clp.tb_seckill_voucher
WHERE stock < 0;

-- 7. 对账异常(应为空)
SELECT biz_type, biz_id, redis_value, db_value, diff, status
FROM clp.tb_reconcile_log
WHERE status = 'PENDING';
