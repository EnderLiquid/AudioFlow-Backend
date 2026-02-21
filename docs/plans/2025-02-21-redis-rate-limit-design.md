# Redis令牌桶限流功能设计文档

## 日期
2025-02-21

## 项目概述

为 AudioFlow 项目实现基于 Redis 的 API 端口限流功能，使用令牌桶算法，支持 IP+账号双重限流维度。主要目标包括：
- 防止暴力破解（登录接口加强限制）
- 保护资源消耗大的接口（如上传歌曲）
- 整体流量控制，防止滥用

## 需求分析

### 功能需求
1. **限流维度**：支持 IP、账号、IP+账号 三种维度
2. **配置灵活**：通过注解为每个接口单独配置限流规则
3. **令牌桶算法**：支持突发流量，避免固定窗口问题
4. **全局默认**：提供默认限流规则，未配置接口使用默认值

### 限流规则
| 接口类型 | 令牌补充速度 | 桶容量 | 限流维度 | 用途 |
|---------|------------|-------|---------|------|
| 登录接口 | 3/60 | 3 | IP+账号 | 防止暴力破解 |
| 上传歌曲 | 3/60 | 3 | 账号 | 控制资源消耗 |
| 普通接口（默认） | 5/1 | 10 | IP+账号 | 通用限流 |

## 技术方案

### 方案选择
采用 **Redis + Lua脚本** 的自定义令牌桶实现

### 方案优势
- 原子性操作：Lua脚本保证令牌扣减和补充的原子性
- 高性能：所有操作在Redis内存中完成
- 无额外依赖：项目已集成Redis
- 灵活扩展：支持自定义注解配置

### 替代方案
- Bucket4j：引入额外依赖，配置复杂
- 纯计数器：无法处理突发流量

## 架构设计

### 整体架构
```
请求 → RequestIdFilter → RateLimitFilter → SaInterceptor → Controller
       ( requestId )       ( 限流检查 )
```

### 组件说明

#### 1. Filter层 - RateLimitFilter
**职责**：全局拦截器，执行限流逻辑
- 位置：RequestIdFilter 之后，SaInterceptor 之前
- 功能：
  - 解析请求方法的 @RateLimit 注解
  - 提取IP地址和用户信息
  - 生成限流Key
  - 调用Redis令牌桶服务检查限流
  - 超限返回 403

#### 2. 注解层 - @RateLimit
**职责**：声明式配置限流规则

```java
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {
    // 令牌补充速度，格式："数值/秒数"，如"5/1"、"3/60"
    String refillRate();

    // 桶容量（最大令牌数）
    String capacity();

    // 限流维度
    LimitType limitType() default LimitType.BOTH;

    // 失败提示信息
    String message() default "请求过于频繁，请稍后再试";
}
```

#### 3. 服务层 - RateLimitService
**职责**：令牌桶算法实现

**核心方法**：
- `tryAcquire(String key, int capacity, double refillRate, int tokens)`：检查并获取令牌
- `parseRefillRate(String rate)`：解析"5/1"格式的补充速度
- `generateKey(String ip, Long userId, String apiPath, LimitType limitType)`：生成限流Key

#### 4. 配置层 - RateLimitProperties
**职责**：管理全局限流默认值

```yaml
rate-limit:
  default:
    refill-rate: "5/1"
    capacity: "10"
```

#### 5. 异常处理 - RateLimitException
**职责**：限流超限异常

```java
public class RateLimitException extends RuntimeException {
    public RateLimitException(String message) {
        super(message);
    }
}
```

在 `GlobalExceptionHandler` 中处理，返回 403 状态码。

## Redis 设计

### Lua 脚本

**输入参数**：
- `keys[1]`：限流标识（Redis key）
- `ARGV[1]`：桶容量
- `ARGV[2]`：令牌补充速度（每秒补充的令牌数）
- `ARGV[3]`：当前时间戳（秒）
- `ARGV[4]`：请求令牌数

**输出**：剩余令牌数（-1 表示超限）

**逻辑**：
1. 获取当前令牌数和上次补充时间
2. 计算应补充的令牌数：`补充数 = (当前时间 - 上次时间) * 补充速度`
3. 更新令牌数：`当前令牌 = min(容量, 原令牌 + 补充数)`
4. 检查是否足够：`当前令牌 >= 请求令牌数`
5. 扣减令牌并更新Redis

脚本位置：`classpath:scripts/rate_limit.lua`

### Redis Key 规则

**格式**：`rate_limit:{type}:{identifier}:{apiPath}`

**示例**：
- IP限流：`rate_limit:ip:192.168.1.1:/api/sessions`
- 用户限流：`rate_limit:user:123:/api/sessions`
- 双重限制：使用两个Key分别限制

**TTL策略**：使用Redis的自动过期机制，或在Lua脚本中主动维护

## 数据流设计

### 限流检查流程
```
1. 请求到达 RateLimitFilter
2. 获取请求路径和方法
3. 检查方法是否标注 @RateLimit
   - 无注解：跳过限流（或使用全局默认）
   - 有注解：解析注解参数
4. 提取限流标识
   - IP：从 X-Real-IP / X-Forwarded-For / RemoteAddr 获取
   - 用户ID：从 Sa-Token 获取（登录用户），未登录为 null
5. 生成 Redis Key
   - 根据 limitType 组合 IP/用户ID/路径
6. 调用 RateLimitService.tryAcquire()
7. 执行 Redis Lua 脚本
   - 成功：返回剩余令牌数
   - 失败：返回 -1
8. 结果处理
   - 成功：放行请求
   - 失败：抛出 RateLimitException，返回 403
```

### IP获取策略
1. 优先检查 `X-Real-IP` 请求头
2. 其次检查 `X-Forwarded-For` 请求头（取第一个IP）
3. 回退到 `request.getRemoteAddr()`

### 用户ID获取
- 通过 `StpUtil.getLoginIdAsLong()` 获取
- 未登录时返回 null（仅IP限流生效）

## 配置说明

### application.yml
```yaml
rate-limit:
  enabled: true
  default:
    refill-rate: "5/1"  # 全局默认：5令牌/秒
    capacity: "10"      # 全局默认：桶容量10
```

### 注解配置示例

**1. 登录接口（严格限制）**
```java
@PostMapping
@RateLimit(
    refillRate = "3/60",
    capacity = "3",
    limitType = LimitType.BOTH,
    message = "登录尝试过于频繁，请稍后再试"
)
public HttpResponseBody<UserVO> login(@Valid @RequestBody UserVerifyPasswordDTO dto) { }
```

**2. 上传歌曲接口（资源消耗大）**
```java
@PostMapping
@RateLimit(
    refillRate = "3/60",
    capacity = "3",
    limitType = LimitType.USER,
    message = "上传过于频繁，请稍后再试"
)
public HttpResponseBody<SongVO> uploadSong(@Valid @ModelAttribute SongSaveDTO dto) { }
```

**3. 普通接口（使用全局默认）**
```java
// 无注解，跳过限流检查
// 或使用全局默认（根据配置决定）
@GetMapping
public HttpResponseBody<CommonPageVO<SongVO>> pageSongs(...) { }
```

## 优先级规则

1. **方法级 @RateLimit 注解**（最高优先级）
2. **全局默认配置**（次之）
3. **不限流**（无配置时）

## 错误处理

### 异常类型
- `RateLimitException`：限流超限异常

### 响应格式
```json
{
  "success": false,
  "message": "登录尝试过于频繁，请稍后再试",
  "data": null,
  "code": 403
}
```

### 日志记录
- 超限事件记录为 `warn` 级别
- 包含信息：IP、用户ID（如有）、接口路径、限流详情

## 测试计划

### 单元测试
1. Lua 脚本执行测试
2. 限流Key生成逻辑测试
3. 补充速度解析测试（"5/1"、"3/60"）

### 集成测试
1. 登录接口限流测试（快速连续请求）
2. 上传接口限流测试
3. 普通接口限流测试
4. IP限流测试（同一IP不同账号）
5. 账号限流测试（同一账号不同IP）
6. 双重限制测试

### 性能测试
- Lua 脚本执行性能
- 高并发场景下的限流准确性
- Redis 连接池压力

## 实现文件结构

```
src/main/java/top/enderliquid/audioflow/
├── common/
│   ├── annotation/
│   │   └── RateLimit.java
│   ├── enums/
│   │   └── LimitType.java
│   ├── exception/
│   │   └── RateLimitException.java
│   ├── filter/
│   │   └── RateLimitFilter.java
│   ├── service/
│   │   └── RateLimitService.java
│   └── config/
│       └── RateLimitProperties.java
└── resources/
    └── scripts/
        └── rate_limit.lua
```

## 安全考虑

1. **XSS防护**：IP地址获取需验证格式合法性
2. **IP伪造防护**：检查代理头，记录多个来源
3. **Redis注入**：Lua脚本参数化，避免动态拼接
4. **日志脱敏**：避免在日志中记录敏感信息

## 性能优化

1. **Lua脚本缓存**：避免重复编译脚本
2. **Redis连接池**：合理配置连接池大小
3. **异步日志**：限流记录使用异步日志
4. **Filter执行顺序**：RateLimitFilter在早期执行，避免不必要的处理

## 扩展性

1. **多级限流**：可扩展支持每分钟、每小时等多级限流
2. **白名单机制**：支持配置IP或用户白名单
3. **动态配置**：通过管理接口动态调整限流规则
4. **熔断降级**：Redis不可用时降级为本地限流
5. **监控指标**：输出限流命中次数到监控系统

## 合规性

所有代码、注释、日志、异常消息使用中文编写。

## 版本历史

| 版本 | 日期 | 作者 | 变更说明 |
|-----|------|------|---------|
| 1.0 | 2025-02-21 | opencode | 初始设计文档 |
