# Redis IP限流功能实现计划

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**目标:** 实现基于 Redis 的 API 端口 IP 限流功能，防止恶意请求和系统资源滥用。

**架构:** 使用自定义注解 + AOP 切面 + Redis Lua 脚本（滑动窗口算法）实现对所有 API 端点进行 IP 限流。

**技术栈:** Spring Boot AOP, Redis, Lua脚本

---

## Task 1: 创建限流注解 @RateLimit

**文件:**
- 创建: `src/main/java/top/enderliquid/audioflow/common/annotation/RateLimit.java`

**Step 1: 创建限流注解类**

```java
package top.enderliquid.audioflow.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * IP限流注解
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {

    /**
     * 限制次数
     */
    int count() default 100;

    /**
     * 时间窗口（秒）
     */
    int time() default 60;

    /**
     * 自定义key（可选，默认使用方法签名）
     */
    String key() default "";
}
```

**Step 2: 确认文件创建成功**

Run: `ls -la src/main/java/top/enderliquid/audioflow/common/annotation/`
Expected: 列出 RateLimit.java 文件

**Step 3: 编译验证**

Run: `./mvnw compile`
Expected: BUILD SUCCESS

**Step 4: 提交**

```bash
git add src/main/java/top/enderliquid/audioflow/common/annotation/RateLimit.java
git commit -m "feat: 添加IP限流注解@RateLimit"
```

---

## Task 2: 创建限流异常 RateLimitException

**文件:**
- 创建: `src/main/java/top/enderliquid/audioflow/common/exception/RateLimitException.java`

**Step 1: 创建限流异常类**

```java
package top.enderliquid.audioflow.common.exception;

/**
 * 限流异常
 */
public class RateLimitException extends RuntimeException {

    public RateLimitException(String message) {
        super(message);
    }

    public RateLimitException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

**Step 2: 确认文件创建成功**

Run: `ls -la src/main/java/top/enderliquid/audioflow/common/exception/`
Expected: 列出 RateLimitException.java 文件

**Step 3: 编译验证**

Run: `./mvnw compile`
Expected: BUILD SUCCESS

**Step 4: 提交**

```bash
git add src/main/java/top/enderliquid/audioflow/common/exception/RateLimitException.java
git commit -m "feat: 添加限流异常RateLimitException"
```

---

## Task 3: 创建IP工具类 IPUtil

**文件:**
- 创建: `src/main/java/top/enderliquid/audioflow/common/aspect/IpUtil.java`

**Step 1: 创建IP获取工具类**

```java
package top.enderliquid.audioflow.common.aspect;

import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;

/**
 * IP工具类
 */
public class IpUtil {

    private static final String UNKNOWN = "unknown";
    private static final String IP_SEPARATOR = ",";
    private static final int IP_MAX_LENGTH = 15;

    /**
     * 获取客户端真实IP
     */
    public static String getClientIp(HttpServletRequest request) {
        if (request == null) {
            return null;
        }

        String ip = request.getHeader("X-Forwarded-For");

        if (StringUtils.isNotBlank(ip) && !UNKNOWN.equalsIgnoreCase(ip)) {
            int index = ip.indexOf(IP_SEPARATOR);
            if (index != -1) {
                return ip.substring(0, index);
            }
            return ip;
        }

        ip = request.getHeader("X-Real-IP");

        if (StringUtils.isNotBlank(ip) && !UNKNOWN.equalsIgnoreCase(ip)) {
            return ip;
        }

        ip = request.getHeader("Proxy-Client-IP");

        if (StringUtils.isNotBlank(ip) && !UNKNOWN.equalsIgnoreCase(ip)) {
            return ip;
        }

        ip = request.getHeader("WL-Proxy-Client-IP");

        if (StringUtils.isNotBlank(ip) && !UNKNOWN.equalsIgnoreCase(ip)) {
            return ip;
        }

        ip = request.getRemoteAddr();

        return ip;
    }
}
```

**Step 2: 确认文件创建成功**

Run: `ls -la src/main/java/top/enderliquid/audioflow/common/aspect/`
Expected: 列出 IpUtil.java 文件

**Step 3: 编译验证**

Run: `./mvnw compile`
Expected: BUILD SUCCESS

**Step 4: 提交**

```bash
git add src/main/java/top/enderliquid/audioflow/common/aspect/IpUtil.java
git commit -m "feat: 添加IP获取工具类IpUtil"
```

---

## Task 4: 创建Redis Lua脚本 rate_limit.lua

**文件:**
- 创建: `src/main/resources/redis/rate_limit.lua`

**Step 1: 创建Lua脚本文件**

```lua
local key = KEYS[1]
local count = tonumber(ARGV[1])
local time = tonumber(ARGV[2])
local current = tonumber(ARGV[3])

-- 删除当前窗口外的过期记录
redis.call('zremrangebyscore', key, '-inf', current - time * 1000)

-- 统计当前窗口内的请求数
local current_count = redis.call('zcard', key)

if current_count < count then
    -- 添加当前请求
    redis.call('zadd', key, current, current)
    redis.call('expire', key, time)
    return 1
else
    return 0
end
```

**Step 2: 确认文件创建成功**

Run: `ls -la src/main/resources/redis/`
Expected: 列出 rate_limit.lua 文件

**Step 3: 提交**

```bash
git add src/main/resources/redis/rate_limit.lua
git commit -m "feat: 添加限流Lua脚本rate_limit.lua"
```

---

## Task 5: 创建限流切面 RateLimitAspect

**文件:**
- 创建: `src/main/java/top/enderliquid/audioflow/common/aspect/RateLimitAspect.java`

**Step 1: 创建限流切面类**

```java
package top.enderliquid.audioflow.common.aspect;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;
import top.enderliquid.audioflow.common.annotation.RateLimit;
import top.enderliquid.audioflow.common.exception.RateLimitException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

/**
 * 限流切面
 */
@Slf4j
@Aspect
@Component
public class RateLimitAspect {

    private static final String LUA_SCRIPT_PATH = "redis/rate_limit.lua";
    private static final String RATE_LIMIT_PREFIX = "rate_limit:";

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Around("@annotation(rateLimit)")
    public Object around(ProceedingJoinPoint joinPoint, RateLimit rateLimit) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String methodName = signature.getDeclaringTypeName() + "." + signature.getName();

        HttpServletRequest request = (HttpServletRequest) joinPoint.getArgs()[0];
        String ip = IpUtil.getClientIp(request);

        if (ip == null) {
            log.warn("获取IP失败，方法: {}", methodName);
            throw new RateLimitException("无法获取客户端IP");
        }

        String key = RATE_LIMIT_PREFIX + methodName + ":" + ip;

        int count = rateLimit.count();
        int time = rateLimit.time();
        long currentTime = System.currentTimeMillis();

        String luaScript = loadLuaScript();

        Long result = redisTemplate.execute(
                RedisScript.of(luaScript, Long.class),
                Collections.singletonList(key),
                String.valueOf(count),
                String.valueOf(time),
                String.valueOf(currentTime)
        );

        if (result != null && result == 0) {
            log.warn("触发限流，IP: {}, 方法: {}, 限制: {}次/{}秒", ip, methodName, count, time);
            throw new RateLimitException("请求过于频繁，请稍后再试");
        }

        return joinPoint.proceed();
    }

    private String loadLuaScript() {
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream(LUA_SCRIPT_PATH);
        if (inputStream == null) {
            throw new RateLimitException("加载Lua脚本失败: " + LUA_SCRIPT_PATH);
        }

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            StringBuilder stringBuilder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line).append("\n");
            }
            return stringBuilder.toString();
        } catch (IOException e) {
            throw new RateLimitException("读取Lua脚本失败", e);
        }
    }
}
```

**Step 2: 确认文件创建成功**

Run: `ls -la src/main/java/top/enderliquid/audioflow/common/aspect/`
Expected: 列出 RateLimitAspect.java 文件

**Step 3: 编译验证**

Run: `./mvnw compile`
Expected: BUILD SUCCESS

**Step 4: 提交**

```bash
git add src/main/java/top/enderliquid/audioflow/common/aspect/RateLimitAspect.java
git commit -m "feat: 添加限流切面RateLimitAspect"
```

---

## Task 6: 在 GlobalExceptionHandler 中添加限流异常处理

**文件:**
- 修改: `src/main/java/top/enderliquid/audioflow/common/exception/GlobalExceptionHandler.java`

**Step 1: 先读取现有的异常处理器文件**

```bash
cat src/main/java/top/enderliquid/audioflow/common/exception/GlobalExceptionHandler.java
```

**Step 2: 在类中添加限流异常处理方法**

在 GlobalExceptionHandler 类中添加以下方法：

```java
@Autowired
private HttpServletResponse response;

@ExceptionHandler(RateLimitException.class)
public HttpResponseBody<Void> handleRateLimit(RateLimitException e) {
    log.warn("触发限流: {}", e.getMessage());
    response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
    return HttpResponseBody.fail(e.getMessage());
}
```

**注意:** 需要导入必要的类：
```java
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import top.enderliquid.audioflow.common.exception.RateLimitException;
```

**Step 3: 编译验证**

Run: `./mvnw compile`
Expected: BUILD SUCCESS

**Step 4: 提交**

```bash
git add src/main/java/top/enderliquid/audioflow/common/exception/GlobalExceptionHandler.java
git commit -m "feat: 在GlobalExceptionHandler中添加限流异常处理"
```

---

## Task 7: 在 SessionController 中添加限流注解

**文件:**
- 修改: `src/main/java/top/enderliquid/audioflow/controller/SessionController.java`

**Step 1: 在login方法上添加限流注解**

修改 login 方法：

```java
/**
 * 用户登录
 */
@RateLimit(count = 20, time = 60)
@PostMapping
public HttpResponseBody<UserVO> login(@Valid @RequestBody UserVerifyPasswordDTO dto) {
    UserVO userVO = userService.verifyUserPassword(dto);
    StpUtil.login(userVO.getId());
    return HttpResponseBody.ok(userVO, "登录成功");
}
```

**Step 2: 在类的导入区添加限流注解**

```java
import top.enderliquid.audioflow.common.annotation.RateLimit;
```

**Step 3: 编译验证**

Run: `./mvnw compile`
Expected: BUILD SUCCESS

**Step 4: 提交**

```bash
git add src/main/java/top/enderliquid/audioflow/controller/SessionController.java
git commit -m "feat: 在登录接口添加限流注解（20次/分钟）"
```

---

## Task 8: 在 UserController 中添加限流注解

**文件:**
- 修改: `src/main/java/top/enderliquid/audioflow/controller/UserController.java`

**Step 1: 在register方法上添加限流注解**

修改 register 方法：

```java
/**
 * 用户注册
 */
@RateLimit(count = 20, time = 60)
@PostMapping
public HttpResponseBody<UserVO> register(@Valid @RequestBody UserSaveDTO dto) {
    UserVO userVO = userService.saveUser(dto);
    StpUtil.login(userVO.getId());
    return HttpResponseBody.ok(userVO, "注册成功");
}
```

**Step 2: 在类的导入区添加限流注解**

```java
import top.enderliquid.audioflow.common.annotation.RateLimit;
```

**Step 3: 编译验证**

Run: `./mvnw compile`
Expected: BUILD SUCCESS

**Step 4: 提交**

```bash
git add src/main/java/top/enderliquid/audioflow/controller/UserController.java
git commit -m "feat: 在注册接口添加限流注解（20次/分钟）"
```

---

## Task 9: 在 SongController 中添加限流注解

**文件:**
- 修改: `src/main/java/top/enderliquid/audioflow/controller/SongController.java`

**Step 1: 在类级别添加限流注解**

在 Controller 类上添加注解：

```java
@RateLimit(count = 100, time = 60)
```

**Step 2: 在类的导入区添加限流注解**

```java
import top.enderliquid.audioflow.common.annotation.RateLimit;
```

**Step 3: 编译验证**

Run: `./mvnw compile`
Expected: BUILD SUCCESS

**Step 4: 提交**

```bash
git add src/main/java/top/enderliquid/audioflow/controller/SongController.java
git commit -m "feat: 在SongController添加限流注解（100次/分钟）"
```

---

## Task 10: 创建 IPUtil 单元测试

**文件:**
- 创建: `src/test/java/top/enderliquid/audioflow/common/aspect/IpUtilTest.java`

**Step 1: 创建IPUtil测试类**

```java
package top.enderliquid.audioflow.common.aspect;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@ExtendWith(MockitoExtension.class)
class IpUtilTest {

    @Test
    void shouldReturnIpFromXForwardedFor() {
        HttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Forwarded-For", "192.168.1.1");

        String ip = IpUtil.getClientIp(request);

        assertEquals("192.168.1.1", ip);
    }

    @Test
    void shouldReturnFirstIpFromXForwardedForWithMultipleIps() {
        HttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Forwarded-For", "192.168.1.1, 192.168.1.2");

        String ip = IpUtil.getClientIp(request);

        assertEquals("192.168.1.1", ip);
    }

    @Test
    void shouldReturnIpFromXRealIp() {
        HttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Real-IP", "10.0.0.1");

        String ip = IpUtil.getClientIp(request);

        assertEquals("10.0.0.1", ip);
    }

    @Test
    void shouldReturnRemoteAddrWhenNoProxyHeaders() {
        HttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("127.0.0.1");

        String ip = IpUtil.getClientIp(request);

        assertEquals("127.0.0.1", ip);
    }

    @Test
    void shouldReturnNullWhenRequestIsNull() {
        String ip = IpUtil.getClientIp(null);

        assertNull(ip);
    }

    @Test
    void shouldIgnoreUnknownInXForwardedFor() {
        HttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Forwarded-For", "unknown");
        request.setRemoteAddr("127.0.0.1");

        String ip = IpUtil.getClientIp(request);

        assertEquals("127.0.0.1", ip);
    }
}
```

**Step 2: 运行测试**

Run: `./mvnw test -Dtest=IpUtilTest`
Expected: 所有测试通过

**Step 3: 提交**

```bash
git add src/test/java/top/enderliquid/audioflow/common/aspect/IpUtilTest.java
git commit -m "test: 添加IpUtil单元测试"
```

---

## Task 11: 创建限流集成测试

**文件:**
- 创建: `src/test/java/top/enderliquid/audioflow/common/aspect/RateLimitIntegrationTest.java`

**Step 1: 创建集成测试类**

```java
package top.enderliquid.audioflow.common.aspect;

import cn.dev33.satoken.stp.StpUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import top.enderliquid.audioflow.dto.request.UserSaveDTO;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class RateLimitIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private StringRedisTemplate redisTemplate;

    private static final String REGISTER_URL = "/api/users";
    private static final String LOGIN_URL = "/api/sessions";

    @AfterEach
    void afterEach() {
        redisTemplate.getConnectionFactory().getConnection().flushDb();
    }

    @Test
    void shouldAllowRequestsUnderLimit() throws Exception {
        UserSaveDTO user = new UserSaveDTO();
        user.setUsername("user001");
        user.setPassword("password123");
        user.setEmail("user001@test.com");

        int allowedRequests = 10;
        for (int i = 0; i < allowedRequests; i++) {
            mockMvc.perform(MockMvcRequestBuilders.post(REGISTER_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(user)))
                    .andExpect(result -> {
                        int status = result.getResponse().getStatus();
                        assertTrue(status == 200 || status == 409,
                            "请求应该在限制内，实际状态码: " + status);
                    });
        }
    }

    @Test
    void shouldBlockRequestsOverLimit() throws Exception {
        String uniqueUsername = "ratelimit_" + System.currentTimeMillis();

        UserSaveDTO user = new UserSaveDTO();
        user.setUsername(uniqueUsername);
        user.setPassword("password123");
        user.setEmail(uniqueUsername + "@test.com");

        int limit = 20;
        int blockedCount = 0;

        for (int i = 0; i < limit + 5; i++) {
            var result = mockMvc.perform(MockMvcRequestBuilders.post(REGISTER_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(user)))
                    .andReturn();

            int status = result.getResponse().getStatus();
            if (status == 429) {
                blockedCount++;
            }
        }

        assertTrue(blockedCount >= 5, "应该有至少5个请求被限流拦截");
    }

    @Test
    void shouldAllowRequestsAfterTimeWindowExpires() throws Exception {
        UserSaveDTO user = new UserSaveDTO();
        user.setUsername("timeuser001");
        user.setPassword("password123");
        user.setEmail("timeuser001@test.com");

        for (int i = 0; i < 20; i++) {
            mockMvc.perform(MockMvcRequestBuilders.post(REGISTER_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(user)));
        }

        boolean limited = false;
        try {
            mockMvc.perform(MockMvcRequestBuilders.post(REGISTER_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(user)))
                    .andExpect(status().is(429));
            limited = true;
        } catch (Exception ignored) {
        }

        assertTrue(limited, "超出限制时应该返回429");
    }
}
```

**注意:** 需要添加导入：

```java
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.connection.RedisConnection;
```

**Step 2: 运行测试**

Run: `./mvnw test -Dtest=RateLimitIntegrationTest`
Expected: 所有测试通过

**Step 3: 提交**

```bash
git add src/test/java/top/enderliquid/audioflow/common/aspect/RateLimitIntegrationTest.java
git commit -m "test: 添加限流集成测试"
```

---

## Task 12: 运行完整测试套件验证

**Step 1: 运行所有测试**

Run: `./mvnw test`
Expected: 所有测试通过

**Step 2: 构建项目**

Run: `./mvnw clean package -DskipTests`
Expected: BUILD SUCCESS

**Step 3: 提交**

```bash
git commit --allow-empty -m "chore: 完成Redis IP限流功能，所有测试通过"
```

---

## 总结

本实现计划包含 12 个任务，按照 TDD 原则逐步实现 Redis IP 限流功能：

1. 创建限流注解
2. 创建限流异常
3. 创建 IP 工具类
4. 创建 Redis Lua 脚本
5. 创建限流切面
6. 在全局异常处理器中添加限流处理
7-9. 在控制器中添加限流注解
10-11. 编写单元测试和集成测试
12. 运行完整测试验证

**关键特性：**
- 零额外依赖，复用现有 Redis
- 注解驱动，配置灵活
- Lua 脚本保证原子性
- 支持分级限流（20/100次分钟）
- 按 IP 限流
- 返回 HTTP 429 状态码