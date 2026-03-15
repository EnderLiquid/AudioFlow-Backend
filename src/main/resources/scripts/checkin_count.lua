-- 原子递增登录计数并设置过期时间
-- KEYS[1]: 计数键
-- ARGV[1]: 过期时间（秒）
-- 返回: 递增后的计数值

local current = redis.call('INCR', KEYS[1])
if current == 1 then
    redis.call('EXPIRE', KEYS[1], ARGV[1])
end
return current