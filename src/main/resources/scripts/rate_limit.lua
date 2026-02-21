-- 令牌桶限流 Lua 脚本
-- KEYS[1]: 限流Key
-- ARGV[1]: 桶容量
-- ARGV[2]: 令牌补充速度（每秒补充的令牌数）
-- ARGV[3]: 当前时间戳（秒）
-- ARGV[4]: 请求令牌数

local key = KEYS[1]
local capacity = tonumber(ARGV[1])
local refillRate = tonumber(ARGV[2])
local now = tonumber(ARGV[3])
local tokensRequested = tonumber(ARGV[4])

-- 获取当前令牌数和上次补充时间
local info = redis.call('hmget', key, 'tokens', 'lastRefillTime')

local currentTokens = tonumber(info[1])
local lastRefillTime = tonumber(info[2])

-- 首次访问，初始化
if not currentTokens then
    currentTokens = capacity
    lastRefillTime = now
end

-- 计算应补充的令牌数
local passedSeconds = now - lastRefillTime
local tokensToAdd = passedSeconds * refillRate

-- 更新令牌数，不超过容量
if tokensToAdd > 0 then
    currentTokens = math.min(capacity, currentTokens + tokensToAdd)
    lastRefillTime = now
end

-- 检查是否有足够令牌
if currentTokens >= tokensRequested then
    -- 扣减令牌
    currentTokens = currentTokens - tokensRequested

    -- 更新Redis
    redis.call('hmset', key, 'tokens', currentTokens, 'lastRefillTime', lastRefillTime)
    redis.call('expire', key, capacity * 2) -- 设置过期时间为容量*2秒

    return currentTokens
else
    -- 令牌不足，返回-1
    return -1
end
