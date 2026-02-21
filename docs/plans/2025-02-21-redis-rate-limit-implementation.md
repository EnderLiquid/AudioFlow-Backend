# Redis令牌桶限流功能实现计划

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 实现基于Redis的API端口限流功能，使用令牌桶算法，支持IP+账号双重限流维度，通过注解灵活配置。

**Architecture:** 采用 Filter + 注解 + AOP 混合模式，Redis Lua脚本实现令牌桶算法，原子性操作保证并发安全。

**Tech Stack:** Java 21, Spring Boot 3.5.10, Redis, MyBatis-Plus, Sa-Token, JUnit 5

---

## 前置准备

### Task 0: 确认项目结构和依赖

**Files:**
- Check: `pom.xml`
- Check: `src/main/resources/application.yml`

**Step 1: 确认Redis依赖已存在**

预期：`pom.xml` 第49行已有 `spring-boot-starter-data-redis` 依赖

**Step 2: 确认Redis配置已存在**

预期：`application.yml` 中已有Redis连接配置

---

## 第一阶段：基础组件

### Task 1: 创建 LimitType 枚举

**Files:**
- Create: `src/main/java/top/enderliquid/audioflow/common/enums/LimitType.java`

**Step 1: 创建枚举文件**

```java
package top.enderliquid.audioflow.common.enums;

import lombok.Getter;

@Getter
public enum LimitType {
    IP,      // 仅限IP
    USER,    // 仅限账号
    BOTH     // IP和账号双重限制
}
```

**Step 2: 编译验证**

运行: `./mvnw clean compile`
预期: 编译成功

**Step 3: 提交**

```bash
git add src/main/java/top/enderliquid/audioflow/common/enums/LimitType.java
git commit -m "feat: 添加LimitType枚举"
```

---

### Task 2: 创建 @RateLimit 注解

**Files:**
- Create: `src/main/java/top/enderliquid/audioflow/common/annotation/RateLimit.java`

**Step 1: 创建注解文件**

```java
package top.enderliquid.audioflow.common.annotation;

import top.enderliquid.audioflow.common.enums.LimitType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {
    String refillRate();

    String capacity();

    LimitType limitType() default LimitType.BOTH;

    String message() default "请求过于频繁，请稍后再试";
}
```

**Step 2: 编译验证**

运行: `./mvnw clean compile`
预期: 编译成功

**Step 3: 提交**

```bash
git add src/main/java/top/enderliquid/audioflow/common/annotation/RateLimit.java
git commit -m "feat: 添加@RateLimit注解"
```

---

### Task 3: 创建 RateLimitException 异常

**Files:**
- Create: `src/main/java/top/enderliquid/audioflow/common/exception/RateLimitException.java`

**Step 1: 创建异常类**

```java
package top.enderliquid.audioflow.common.exception;

public class RateLimitException extends RuntimeException {
    public RateLimitException(String message) {
        super(message);
    }
}
```

**Step 2: 编译验证**

运行: `./mvnw clean compile`
预期: 编译成功

**Step 3: 提交**

```bash
git add src/main/java/top/enderliquid/audioflow/common/exception/RateLimitException.java
git commit -m "feat: 添加RateLimitException异常"
```

---

### Task 4: 在 GlobalExceptionHandler 中处理限流异常

**Files:**
- Modify: `src/main/java/top/enderliquid/audioflow/common/exception/GlobalExceptionHandler.java`

**Step 1: 读取现有异常处理器**

运行: `cat src/main/java/top/enderliquid/audioflow/common/exception/GlobalExceptionHandler.java`

**Step 2: 添加异常处理方法**

在类中增加：

```java
@ExceptionHandler(RateLimitException.class)
public HttpResponseBody<Void> handleRateLimitException(RateLimitException e) {
    return HttpResponseBody.fail(e.getMessage(), null, 403);
}
```

**Step 3: 添加导入**

```java
import top.enderliquid.audioflow.common.exception.RateLimitException;
```

**Step 4: 编译验证**

运行: `./mvnw clean compile`
预期: 编译成功

**Step 5: 提交**

```bash
git add src/main/java/top/enderliquid/audioflow/common/exception/GlobalExceptionHandler.java
git commit -m "feat: 添加RateLimitException异常处理"
```

---

### Task 5: 创建 RateLimitProperties 配置类

**Files:**
- Create: `src/main/java/top/enderliquid/audioflow/common/config/RateLimitProperties.java`

**Step 1: 创建配置类**

```java
package top.enderliquid.audioflow.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "rate-limit")
public class RateLimitProperties {
    private DefaultConfig default = new DefaultConfig();

    @Data
    public static class DefaultConfig {
        private String refillRate = "5/1";
        private String capacity = "10";
    }
}
```

**Step 2: 在 application.yml 添加配置**

在 `src/main/resources/application.yml` 添加：

```yaml
rate-limit:
  default:
    refill-rate: "5/1"
    capacity: "10"
```

**Step 3: 编译验证**

运行: `./mvnw clean compile`
预期: 编译成功

**Step 4: 提交**

```bash
git add src/main/java/top/enderliquid/audioflow/common/config/RateLimitProperties.java
git add src/main/resources/application.yml
git commit -m "feat: 添加限流配置类和默认配置"
```

---

## 第二阶段：Redis Lua脚本

### Task 6: 创建令牌桶 Lua 脚本

**Files:**
- Create: `src/main/resources/scripts/rate_limit.lua`

**Step 1: 创建 Lua 脚本**

```lua
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
local info = redis.call('hmget', key, 'tokens', 'lastRefill')

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
```

**Step 2: 提交**

```bash
git add src/main/resources/scripts/rate_limit.lua
git commit -m "feat: 添加令牌桶Lua脚本"
```

---

## 第三阶段：服务层

### Task 7: 创建 RateLimitService 服务类

**Files:**
- Create: `src/main/java/top/enderliquid/audioflow/common/service/RateLimitService.java`

**Step 1: 创建服务类框架**

```java
package top.enderliquid.audioflow.common.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;
import top.enderliquid.audioflow.common.enums.LimitType;

import jakarta.annotation.PostConstruct;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class RateLimitService {

    @Autowired
    private StringRedisTemplate redisTemplate;

    private RedisScript<Long> rateLimitScript;

    @PostConstruct
    public void init() {
        List<String> keys = Collections.singletonList("rate_limit");
        String script = "rate_limit.lua";
        rateLimitScript = new DefaultRedisScript<>(script, Long.class);
    }

    public boolean tryAcquire(String key, int capacity, double refillRate, int tokensRequested) {
        long now = System.currentTimeMillis() / 1000;
        Long result = redisTemplate.execute(
            rateLimitScript,
            Collections.singletonList(key),
            String.valueOf(capacity),
            String.valueOf(refillRate),
            String.valueOf(now),
            String.valueOf(tokensRequested)
        );

        if (result == null || result < 0) {
            log.warn("限流检查失败，key: {}, capacity: {}, refillRate: {}", key, capacity, refillRate);
            return false;
        }

        log.debug("限流检查通过，key: {}, 剩余令牌: {}", key, result);
        return true;
    }

    public double parseRefillRate(String rate) {
        String[] parts = rate.split("/");
        if (parts.length != 2) {
            throw new IllegalArgumentException("补充速度格式错误: " + rate);
        }
        double tokens = Double.parseDouble(parts[0]);
        double seconds = Double.parseDouble(parts[1]);
        return tokens / seconds;
    }

    public String generateKey(String ip, Long userId, String apiPath, LimitType limitType) {
        StringBuilder keyBuilder = new StringBuilder("rate_limit:");

        switch (limitType) {
            case IP:
                keyBuilder.append("ip:").append(ip);
                break;
            case USER:
                if (userId == null) {
                    throw new IllegalArgumentException("USER限流需要登录");
                }
                keyBuilder.append("user:").append(userId);
                break;
            case BOTH:
                keyBuilder.append("both:").append(ip).append(":").append(userId);
                break;
            default:
                throw new IllegalArgumentException("不支持的限流类型: " + limitType);
        }

        keyBuilder.append(":").append(apiPath);
        return keyBuilder.toString();
    }
}
```

**注意**: 这里Lua脚本加载方式需要调整（后续Task修正）。

**Step 2: 编译验证**

运行: `./mvnw clean compile`
预期: 编译成功（可能有警告，暂时忽略）

**Step 3: 提交**

```bash
git add src/main/java/top/enderliquid/audioflow/common/service/RateLimitService.java
git commit -m "feat: 添加RateLimitService服务类（初稿）"
```

---

### Task 8: 修正 RateLimitService 的 Lua 脚本加载

**Files:**
- Modify: `src/main/java/top/enderliquid/audioflow/common/service/RateLimitService.java`

**Step 1: 修正脚本加载方式**

将 init() 方法改为：

```java
@PostConstruct
public void init() {
    String scriptPath = "classpath:scripts/rate_limit.lua";
    Resource resource = new ClassPathResource("scripts/rate_limit.lua");
    try {
        String script = new String(resource.getContentAsByteArray());
        rateLimitScript = new DefaultRedisScript<>(script, Long.class);
    } catch (IOException e) {
        log.error("加载Lua脚本失败", e);
        throw new RuntimeException("加载Lua脚本失败", e);
    }
}
```

**Step 2: 添加导入**

```java
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import java.io.IOException;
```

**Step 3: 编译验证**

运行: `./mvnw clean compile`
预期: 编译成功

**Step 4: 提交**

```bash
git add src/main/java/top/enderliquid/audioflow/common/service/RateLimitService.java
git commit -m "fix: 修正Lua脚本加载方式"
```

---

## 第四阶段：过滤器

### Task 9: 创建 RateLimitFilter 过滤器

**Files:**
- Create: `src/main/java/top/enderliquid/audioflow/common/filter/RateLimitFilter.java`

**Step 1: 创建过滤器框架**

```java
package top.enderliquid.audioflow.common.filter;

import cn.dev33.satoken.stp.StpUtil;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import top.enderliquid.audioflow.common.annotation.RateLimit;
import top.enderliquid.audioflow.common.enums.LimitType;
import top.enderliquid.audioflow.common.exception.RateLimitException;
import top.enderliquid.audioflow.common.service.RateLimitService;

import java.io.IOException;

@Slf4j
@Component
@WebFilter(urlPatterns = "/*")
public class RateLimitFilter implements Filter {

    @Autowired
    private RateLimitService rateLimitService;

    @Autowired
    private RequestMappingHandlerMapping handlerMapping;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;

        try {
            RateLimit rateLimit = findRateLimitAnnotation(httpRequest);
            if (rateLimit == null) {
                chain.doFilter(request, response);
                return;
            }

            applyRateLimit(httpRequest, rateLimit);
            chain.doFilter(request, response);
        } catch (RateLimitException e) {
            handleRateLimitException((jakarta.servlet.http.HttpServletResponse) response, e);
        }
    }

    private RateLimit findRateLimitAnnotation(HttpServletRequest request) {
        try {
            HandlerExecutionChain handlerChain = handlerMapping.getHandler(request);
            if (handlerChain == null) {
                return null;
            }

            Object handler = handlerChain.getHandler();
            if (handler instanceof HandlerMethod) {
                HandlerMethod handlerMethod = (HandlerMethod) handler;
                return AnnotationUtils.findAnnotation(handlerMethod.getMethod(), RateLimit.class);
            }
        } catch (Exception e) {
            log.warn("查找@RateLimit注解失败", e);
        }
        return null;
    }

    private void applyRateLimit(HttpServletRequest request, RateLimit rateLimit) {
        String ip = getClientIp(request);
        Long userId = getUserId();
        String apiPath = request.getRequestURI();

        String refillRate = rateLimit.refillRate();
        String capacity = rateLimit.capacity();
        LimitType limitType = rateLimit.limitType();

        int capacityInt = Integer.parseInt(capacity);
        double refillRateDouble = rateLimitService.parseRefillRate(refillRate);

        if (limitType == LimitType.BOTH) {
            String ipKey = rateLimitService.generateKey(ip, userId, apiPath, LimitType.IP);
            if (!rateLimitService.tryAcquire(ipKey, capacityInt, refillRateDouble, 1)) {
                throw new RateLimitException(rateLimit.message());
            }
        }

        if (limitType == LimitType.USER || limitType == LimitType.BOTH) {
            String userKey = rateLimitService.generateKey(ip, userId, apiPath, LimitType.USER);
            if (!rateLimitService.tryAcquire(userKey, capacityInt, refillRateDouble, 1)) {
                throw new RateLimitException(rateLimit.message());
            }
        }

        if (limitType == LimitType.IP) {
            String ipKey = rateLimitService.generateKey(ip, userId, apiPath, LimitType.IP);
            if (!rateLimitService.tryAcquire(ipKey, capacityInt, refillRateDouble, 1)) {
                throw new RateLimitException(rateLimit.message());
            }
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Real-IP");
        if (ip == null || ip.isEmpty()) {
            ip = request.getHeader("X-Forwarded-For");
            if (ip != null && !ip.isEmpty()) {
                ip = ip.split(",")[0].trim();
            }
        }
        if (ip == null || ip.isEmpty()) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }

    private Long getUserId() {
        try {
            return StpUtil.getLoginIdAsLong();
        } catch (Exception e) {
            return null;
        }
    }

    private void handleRateLimitException(jakarta.servlet.http.HttpServletResponse response, RateLimitException e) throws IOException {
        response.setStatus(403);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(
            "{\"success\":false,\"message\":\"" + e.getMessage() + "\",\"data\":null,\"code\":403}"
        );
    }
}
```

**Step 2: 添加导入**

```java
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
```

**Step 3: 编译验证**

运行: `./mvnw clean compile`
预期: 编译成功

**Step 4: 提交**

```bash
git add src/main/java/top/enderliquid/audioflow/common/filter/RateLimitFilter.java
git commit -m "feat: 添加RateLimitFilter过滤器"
```

---

## 第五阶段：应用限流规则

### Task 10: 在登录接口应用限流

**Files:**
- Modify: `src/main/java/top/enderliquid/audioflow/controller/SessionController.java`

**Step 1: 添加 @RateLimit 注解到登录方法**

```java
@PostMapping
@RateLimit(
    refillRate = "3/60",
    capacity = "3",
    limitType = LimitType.BOTH,
    message = "登录尝试过于频繁，请稍后再试"
)
public HttpResponseBody<UserVO> login(@Valid @RequestBody UserVerifyPasswordDTO dto) {
```

**Step 2: 添加导入**

```java
import top.enderliquid.audioflow.common.annotation.RateLimit;
import top.enderliquid.audioflow.common.enums.LimitType;
```

**Step 3: 编译验证**

运行: `./mvnw clean compile`
预期: 编译成功

**Step 4: 提交**

```bash
git add src/main/java/top/enderliquid/audioflow/controller/SessionController.java
git commit -m "feat: 登录接口添加限流规则（3次/分钟）"
```

---

### Task 11: 在注册接口应用限流

**Files:**
- Modify: `src/main/java/top/enderliquid/audioflow/controller/UserController.java`

**Step 1: 添加 @RateLimit 注解到注册方法**

```java
@PostMapping
@RateLimit(
    refillRate = "5/1",
    capacity = "10",
    limitType = LimitType.BOTH,
    message = "注册过于频繁，请稍后再试"
)
public HttpResponseBody<UserVO> register(@Valid @RequestBody UserSaveDTO dto) {
```

**Step 2: 添加导入**

```java
import top.enderliquid.audioflow.common.annotation.RateLimit;
import top.enderliquid.audioflow.common.enums.LimitType;
```

**Step 3: 编译验证**

运行: `./mvnw clean compile`
预期: 编译成功

**Step 4: 提交**

```bash
git add src/main/java/top/enderliquid/audioflow/controller/UserController.java
git commit -m "feat: 注册接口添加限流规则（5次/秒）"
```

---

### Task 12: 在上传歌曲接口应用限流

**Files:**
- Modify: `src/main/java/top/enderliquid/audioflow/controller/SongController.java`

**Step 1: 添加 @RateLimit 注解到上传方法**

```java
@SaCheckLogin
@PostMapping
@RateLimit(
    refillRate = "3/60",
    capacity = "3",
    limitType = LimitType.USER,
    message = "上传过于频繁，请稍后再试"
)
public HttpResponseBody<SongVO> uploadSong(@Valid @ModelAttribute SongSaveDTO dto) {
```

**Step 2: 添加导入**

```java
import top.enderliquid.audioflow.common.annotation.RateLimit;
import top.enderliquid/audioflow.common.enums.LimitType;
```

**Step 3: 编译验证**

运行: `./mvnw clean compile`
预期: 编译成功

**Step 4: 提交**

```bash
git add src/main/java/top/enderliquid/audioflow/controller/SongController.java
git commit -m "feat: 上传歌曲接口添加限流规则（3次/分钟）"
```

---

## 第六阶段：测试

### Task 13: 创建 RateLimitService 测试

**Files:**
- Create: `src/test/java/top/enderliquid/audioflow/common/service/RateLimitServiceTest.java`

**Step 1: 创建测试类**

```java
package top.enderliquid.audioflow.common.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import top.enderliquid.audioflow.common.enums.LimitType;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class RateLimitServiceTest {

    @InjectMocks
    private RateLimitService rateLimitService;

    @Test
    void testParseRefillRate_Success() {
        double rate = rateLimitService.parseRefillRate("5/1");
        assertEquals(5.0, rate);
    }

    @Test
    void testParseRefillRate_PerMinute() {
        double rate = rateLimitService.parseRefillRate("3/60");
        assertEquals(0.05, rate, 0.001);
    }

    @Test
    void testParseRefillRate_InvalidFormat() {
        assertThrows(IllegalArgumentException.class, () -> {
            rateLimitService.parseRefillRate("invalid");
        });
    }

    @Test
    void testGenerateKey_IP() {
        String key = rateLimitService.generateKey("192.168.1.1", null, "/api/sessions", LimitType.IP);
        assertEquals("rate_limit:ip:192.168.1.1:/api/sessions", key);
    }

    @Test
    void testGenerateKey_USER() {
        String key = rateLimitService.generateKey("192.168.1.1", 123L, "/api/songs", LimitType.USER);
        assertEquals("rate_limit:user:123:/api/songs", key);
    }

    @Test
    void testGenerateKey_USER_NotLogin() {
        assertThrows(IllegalArgumentException.class, () -> {
            rateLimitService.generateKey("192.168.1.1", null, "/api/songs", LimitType.USER);
        });
    }

    @Test
    void testGenerateKey_BOTH() {
        String key = rateLimitService.generateKey("192.168.1.1", 123L, "/api/sessions", LimitType.BOTH);
        assertEquals("rate_limit:both:192.168.1.1:123:/api/sessions", key);
    }
}
```

**Step 2: 运行测试**

运行: `./mvnw test -Dtest=RateLimitServiceTest`
预期: 所有测试通过

**Step 3: 提交**

```bash
git add src/test/java/top/enderliquid/audioflow/common/service/RateLimitServiceTest.java
git commit -m "test: 添加RateLimitService单元测试"
```

---

### Task 14: 创建 RateLimitFilter 集成测试

**Files:**
- Create: `src/test/java/top/enderliquid/audioflow/common/filter/RateLimitFilterIntegrationTest.java`

**Step 1: 创建集成测试类**

```java
package top.enderliquid.audioflow.common.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class RateLimitFilterIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void testUnlimitedEndpoint() throws Exception {
        mockMvc.perform(get("/api/users/me"))
            .andExpect(status().isOk());
    }
}
```

**Step 2: 运行测试**

运行: `./mvnw test -Dtest=RateLimitFilterIntegrationTest`
预期: 测试通过（注意：需要登录状态，可能需调整）

**Step 3: 提交**

```bash
git add src/test/java/top/enderliquid/audioflow/common/filter/RateLimitFilterIntegrationTest.java
git commit -m "test: 添加RateLimitFilter集成测试"
```

---

### Task 15: 创建 Lua 脚本单元测试

**Files:**
- Create: `src/test/java/top/enderliquid/audioflow/common/service/RateLimitLuaScriptTest.java`

**Step 1: 创建测试类**

```java
package top.enderliquid.audioflow.common.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class RateLimitLuaScriptTest {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Test
    void testLuaScriptExecution() {
        String key = "test_rate_limit";

        long result = redisTemplate.execute(
            new org.springframework.data.redis.core.script.DefaultRedisScript<>(
                "return 1",
                Long.class
            ),
            java.util.Collections.singletonList(key)
        );

        assertEquals(1L, result);
    }
}
```

**Step 2: 运行测试**

运行: `./mvnw test -Dtest=RateLimitLuaScriptTest`
预期: 测试通过

**Step 3: 提交**

```bash
git add src/test/java/top/enderliquid/audioflow/common/service/RateLimitLuaScriptTest.java
git commit -m "test: 添加Lua脚本执行测试"
```

---

## 第七阶段：文档和验证

### Task 16: 更新 README 文档

**Files:**
- Modify: `README.md` (如果存在) 或 `docs/plans/2025-02-21-redis-rate-limit-design.md`

**Step 1: 在实现文档中添加使用说明**

在 `docs/plans/2025-02-21-redis-rate-limit-design.md` 末尾添加：

```markdown
## 使用说明

### 启用限流功能
1. 确保 Redis 服务已启动并配置正确
2. 在 application.yml 中配置限流默认值
3. 在需要限流的接口添加 @RateLimit 注解

### 示例

#### 登录接口（3次/分钟）
```java
@PostMapping
@RateLimit(refillRate = "3/60", capacity = "3", limitType = LimitType.BOTH)
public HttpResponseBody<UserVO> login(@RequestBody UserVerifyPasswordDTO dto) { }
```

#### 上传接口（3次/分钟）
```java
@PostMapping
@RateLimit(refillRate = "3/60", capacity = "3", limitType = LimitType.USER)
public HttpResponseBody<SongVO> uploadSong(@ModelAttribute SongSaveDTO dto) { }
```

### 配置参数说明

- refillRate: 令牌补充速度，格式为"数值/秒数"
  - "5/1" = 每秒补充5个令牌
  - "3/60" = 每分钟补充3个令牌

- capacity: 桶容量（最大令牌数）

- limitType: 限流维度
  - IP: 仅限IP地址
  - USER: 仅限用户账号
  - BOTH: IP和账号双重限制

### 监控和日志
- 限流请求会记录 WARN 级别日志
- 可通过日志分析限流命中情况
```

**Step 2: 提交**

```bash
git add docs/plans/2025-02-21-redis-rate-limit-design.md
git commit -m "docs: 添加限流功能使用说明"
```

---

### Task 17: 最终验证测试

**Files:**
- Check: 所有修改的文件

**Step 1: 运行完整测试套件**

运行: `./mvnw test`
预期: 所有测试通过

**Step 2: 编译打包**

运行: `./mvnw clean package -DskipTests`
预期: 打包成功

**Step 3: 提交最终版本**

```bash
git add .
git commit -m "feat: 完成Redis令牌桶限流功能实现"
```

---

## 总结

### 实现文件清单

**新增文件**：
- `src/main/java/top/enderliquid/audioflow/common/enums/LimitType.java`
- `src/main/java/top/enderliquid/audioflow/common/annotation/RateLimit.java`
- `src/main/java/top/enderliquid/audioflow/common/exception/RateLimitException.java`
- `src/main/java/top/enderliquid/audioflow/common/config/RateLimitProperties.java`
- `src/main/java/top/enderliquid/audioflow/common/service/RateLimitService.java`
- `src/main/java/top/enderliquid/audioflow/common/filter/RateLimitFilter.java`
- `src/main/resources/scripts/rate_limit.lua`
- `src/test/java/top/enderliquid/audioflow/common/service/RateLimitServiceTest.java`
- `src/test/java/top/enderliquid/audioflow/common/filter/RateLimitFilterIntegrationTest.java`
- `src/test/java/top/enderliquid/audioflow/common/service/RateLimitLuaScriptTest.java`

**修改文件**：
- `src/main/java/top/enderliquid/audioflow/common/exception/GlobalExceptionHandler.java`
- `src/main/java/top/enderliquid/audioflow/controller/SessionController.java`
- `src/main/java/top/enderliquid/audioflow/controller/UserController.java`
- `src/main/java/top/enderliquid/audioflow/controller/SongController.java`
- `src/main/resources/application.yml`

### 功能特性

✅ IP+账号双重限流维度
✅ 令牌桶算法（支持突发流量）
✅ 注解式配置（灵活可扩展）
✅ 全局默认配置
✅ Redis Lua脚本（原子性操作）
✅ 详细日志记录
✅ 完整单元测试

### 后续优化建议

1. 添加监控指标（Prometheus）
2. 支持动态配置刷新
3. 添加白名单机制
4. 支持更复杂的限流规则（如分时段限流）
5. 添加限流统计Dashboard
6. 考虑降级方案（Redis不可用时）
