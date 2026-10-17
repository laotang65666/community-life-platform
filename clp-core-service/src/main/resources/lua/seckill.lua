-- 秒杀原子脚本
-- KEYS[1] 库存 key          clp:seckill:stock:{voucherId}
-- KEYS[2] 已购用户 Set      clp:seckill:users:{voucherId}
-- KEYS[3] 令牌 key          clp:seckill:token:{token}
-- ARGV[1] 当前用户 ID
--
-- 返回:
--   0 成功
--   1 库存不足
--   2 重复购买(一人一单)
--   3 令牌无效或已使用
--
-- 整段脚本在 Redis 中原子执行,中间不会被其他命令插入。

-- 1. 校验令牌,并立即消费(一次性)
local tokenUserId = redis.call('GET', KEYS[3])
if (not tokenUserId) or (tokenUserId ~= ARGV[1]) then
    return 3
end
redis.call('DEL', KEYS[3])

-- 2. 一人一单
if redis.call('SISMEMBER', KEYS[2], ARGV[1]) == 1 then
    return 2
end

-- 3. 库存
local stock = tonumber(redis.call('GET', KEYS[1]))
if (not stock) or (stock <= 0) then
    return 1
end

-- 4. 扣减 + 记录用户
redis.call('DECR', KEYS[1])
redis.call('SADD', KEYS[2], ARGV[1])

return 0
