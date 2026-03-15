-- 原子添加元素到HyperLogLog并设置过期时间
-- KEYS[1]: HyperLogLog键
-- ARGV[1]: 元素值
-- ARGV[2]: 过期时间（秒）
-- 返回: 1表示新元素添加成功，0表示元素已存在

local added = redis.call('PFADD', KEYS[1], ARGV[1])
if added == 1 then
    redis.call('EXPIRE', KEYS[1], ARGV[2])
end
return added