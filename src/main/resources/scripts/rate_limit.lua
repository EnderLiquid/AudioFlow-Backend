-- 令牌桶限流 Lua 脚本
--
-- KEYS[1]: 限流 Key
-- ARGV[1]: 桶容量 (Capacity)
-- ARGV[2]: 令牌生成速率 (Rate, 单位: 个/秒)
-- ARGV[3]: 当前时间戳 (毫秒)
-- ARGV[4]: 本次请求需要的令牌数

local key = KEYS[1]
local capacity = tonumber(ARGV[1])
local rate = tonumber(ARGV[2])
local now = tonumber(ARGV[3])
local requested = tonumber(ARGV[4])

-- 1. 获取当前桶内令牌数和上次刷新时间
local info = redis.call('hmget', key, 'tokens', 'last_ts')
local last_tokens = tonumber(info[1])
local last_ts = tonumber(info[2])

-- 2. 初始化：首次访问或 Key 已过期，视为满桶
if last_tokens == nil then
    last_tokens = capacity
    last_ts = now
end

-- 3. 计算距离上次请求经过的时间 (毫秒)
-- 使用 math.max 防止系统时钟回拨导致计算异常
local delta_ms = math.max(0, now - last_ts)

-- 4. 计算这段时间应补充的令牌数
-- 公式：(经过毫秒数 / 1000) * 每秒速率
local filled_tokens = delta_ms * (rate / 1000)

-- 5. 计算当前总令牌数 (补充后不能超过容量)
local current_tokens = math.min(capacity, last_tokens + filled_tokens)

-- 6. 核心判断：令牌是否足够
local allowed = false
if current_tokens >= requested then
    -- 扣减令牌
    current_tokens = current_tokens - requested
    allowed = true
end

-- 7. 更新 Redis 状态
-- 无论成功与否，都更新当前令牌数和时间戳，确保下一次计算基于最新状态
redis.call('hmset', key, 'tokens', current_tokens, 'last_ts', now)

-- 8. 设置 Key 过期时间 (自动清理长期不用的 Key)
-- 策略：设置为“填满桶所需时间”的 2 倍，且保底 60 秒，防止在低频限流场景下 Key 意外过期
local ttl = math.ceil((capacity / rate) * 2)
if ttl < 60 then ttl = 60 end
redis.call('expire', key, ttl)

-- 9. 返回结果
-- 成功：返回剩余令牌数 (>=0)
-- 失败：返回 -1
if allowed then
    return current_tokens
else
    return -1
end
