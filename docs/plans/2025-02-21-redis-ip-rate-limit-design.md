# Redis IP限流功能设计文档

## 1. 需求概述

### 1.1 目标
实现基于 Redis 的 API 端口 IP 限流功能，防止恶意请求和系统资源滥用。

### 1.2 限流范围
全部 11 个 API 端点：
- `/api/sessions` - 登录/注销
- `/api/users` - 注册/用户信息/密码修改
- `/api/songs` - 歌曲增删改查/上传/播放

### 1.3 限流策略
- **敏感接口**（登录/注册）：20 次/分钟
- **普通接口**：100 次/分钟

### 1.4 限流粒度
按 IP 地址限流（防止单个 IP 滥用）

### 1.5 超限处理
返回 HTTP 429 状态码

## 2. 技术方案

### 2.1 核心组件

| 组件 | 职责 |
|------|------|
| `@RateLimit` 注解 | 标记需限流的端点，配置限流参数 |
| `RateLimitAspect` | AOP 切面，拦截方法并执行限流逻辑 |
| `IPUtil` | 工具类，获取客户端真实 IP |
| `RateLimitException` | 自定义异常，触发 429 响应 |
| `rate_limit.lua` | Redis Lua 脚本，实现滑动窗口算法 |

### 2.2 限流算法
采用 **滑动时间窗口算法**，使用 Redis Sorted Set 实现：
- 将每个请求的时间戳作为分数存入 ZSET
- 定期清理超出时间窗口的记录
- 统计窗口内记录数与阈值比较

### 2.3 Redis Key 设计
```
rate_limit:{endpoint}:{ip}:{timestamp_window}
```

## 3. 架构设计

### 3.1 项目结构
```
src/main/java/top/enderliquid/audioflow/
├── common/
│   ├── aspect/
│   │   ├── RateLimitAspect.java          // AOP切面
│   │   └── IpUtil.java                    // IP工具类
│   ├── exception/
│   │   └── RateLimitException.java        // 限流异常
│   └── annotation/
│       └── RateLimit.java                 // 限流注解
├── controller/
│   ├── UserController.java                // 添加 @RateLimit 注解
│   ├── SessionController.java
│   └── SongController.java
├── common/exception/GlobalExceptionHandler.java  // 增强异常处理
└── resources/
    └── redis/
        └── rate_limit.lua                // Lua脚本
```

### 3.2 注解定义
```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {
    int count() default 100;        // 限制次数
    int time() default 60;         // 时间窗口（秒）
    String key() default "";       // 自定义key（可选）
}
```

### 3.3 Redis Lua 脚本
```lua
local key = KEYS[1]
local count = tonumber(ARGV[1])
local time = tonumber(ARGV[2])
local current = tonumber(ARGV[3])

redis.call('zremrangebyscore', key, '-inf', current - time * 1000)
local current_count = redis.call('zcard', key)

if current_count < count then
    redis.call('zadd', key, current, current)
    redis.call('expire', key, time)
    return 1
else
    return 0
end
```

## 4. 实现细节

### 4.1 IP 获取策略
1. 优先检查 `X-Forwarded-For` 代理头
2. 其次检查 `X-Real-IP` 代理头
3. 最后使用 `request.getRemoteAddr()`
4. 提取第一个非 `unknown` 的 IP 地址

### 4.2 控制器注解配置

```java
// SessionController.java - 登录
@PostMapping
@RateLimit(count = 20, time = 60)
public HttpResponseBody<UserVO> login(...) { }

// UserController.java - 注册
@PostMapping
@RateLimit(count = 20, time = 60)
public HttpResponseBody<UserVO> register(...) { }

// 其他所有端点默认使用 100次/分钟
@RateLimit(count = 100, time = 60)
```

### 4.3 异常处理
在 `GlobalExceptionHandler` 中添加：
```java
@ExceptionHandler(RateLimitException.class)
public ResponseEntity<HttpResponseBody> handleRateLimit(RateLimitException e) {
    return ResponseEntity
        .status(HttpStatus.TOO_MANY_REQUESTS)
        .body(HttpResponseBody.fail("请求过于频繁，请稍后再试"));
}
```

## 5. 依赖说明

项目已包含以下依赖，无需新增：
- `spring-boot-starter-data-redis` (pom.xml:49)
- Redis 配置已完成（用于 Sa-Token 存储会话）

## 6. 测试计划

### 6.1 单元测试
- `IPUtilTest` - 测试 IP 获取逻辑
- `RateLimitAspectTest` - 测试限流切面逻辑

### 6.2 集成测试
- 使用 `MockHttpServletRequest` 模拟请求
- 验证限流生效（超过阈值返回 429）
- 验证时间窗口到期后可正常请求

## 7. 优势与风险评估

### 7.1 优势
- 零额外依赖，复用现有 Redis
- 注解驱动，配置灵活
- 采用 Lua 脚本保证原子性
- 符合项目现有架构规范

### 7.2 风险与应对
| 风险 | 应对措施 |
|------|----------|
| Redis 故障导致限流失效 | 降级策略：记录日志但不阻断请求 |
| 绕过代理头伪造 IP | 结合网络层防火墙防护 |
| 分布式环境下 IP 限流不精准 | 后续可升级为按用户+IP双重限流 |

## 8. 扩展性

预留以下扩展接口：
- 支持自定义限流策略（令牌桶、漏桶等）
- 支持按用户 ID 限流（基于 Sa-Token）
- 支持配置化的多维度限流规则
- 支持限流监控和告警