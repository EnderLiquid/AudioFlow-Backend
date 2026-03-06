# 限流注解组合化改造实现计划

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 将 @RateLimit 单一注解改造为组合注解，支持 IP/用户/全局三种限流维度的自由搭配组合，每种维度可独立配置令牌池。

**Architecture:** 采用容器注解模式，@RateLimits 作为容器注解包含多个 @RateLimit 单一规则注解。每个 @RateLimit 配置一种限流维度的参数。Aspect 层遍历所有限流规则依次验证。

**Tech Stack:** Spring AOP, MyBatis-Plus, Redis (令牌桶算法)

---

## Task 1: 修改 LimitType 枚举

**Files:**
- Modify: `src/main/java/top/enderliquid/audioflow/common/enums/LimitType.java`

**Step 1: 修改 LimitType 枚举定义**

移除 BOTH 枚举值，新增 GLOBAL 枚举值，并添加中文注释。

```java
package top.enderliquid.audioflow.common.enums;

import lombok.Getter;

@Getter
public enum LimitType {
    IP,      // IP 维度的限流
    USER,    // 用户维度的限流
    GLOBAL   // 接口总访问限制（不区分 IP/用户）
}
```

**Step 2: 验证枚举修改正确**

Run: `./mvnw compile -DskipTests`
Expected: BUILD SUCCESS

**Step 3: 提交**

```bash
git add src/main/java/top/enderliquid/audioflow/common/enums/LimitType.java
git commit -m "修改限流类型枚举：移除 BOTH，新增 GLOBAL"
```

---

## Task 2: 创建 RateLimits 容器注解

**Files:**
- Create: `src/main/java/top/enderliquid/audioflow/common/annotation/RateLimits.java`

**Step 1: 创建 RateLimits 容器注解**

```java
package top.enderliquid.audioflow.common.annotation;

import java.lang.annotation.*;

/**
 * 限流容器注解 - 支持多个限流规则的自由组合
 */
@Documented
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimits {
    /**
     * 限流规则数组，支持配置多个限流维度
     */
    RateLimit[] value();
    
    /**
     * 限流触发时的错误消息，所有限流维度共享
     */
    String message() default "请求过于频繁，请稍后再试";
    
    /**
     * 入口标识，用于 Redis key 前缀，为空则使用方法名
     */
    String entry() default "";
}
```

**Step 2: 验证注解创建正确**

Run: `./mvnw compile -DskipTests`
Expected: BUILD SUCCESS

**Step 3: 提交**

```bash
git add src/main/java/top/enderliquid/audioflow/common/annotation/RateLimits.java
git commit -m "新增 RateLimits 容器注解"
```

---

## Task 3: 修改 RateLimit 注解

**Files:**
- Modify: `src/main/java/top/enderliquid/audioflow/common/annotation/RateLimit.java`

**Step 1: 重构 RateLimit 注解**

将 RateLimit 改造为单一规则注解，移除 limitType/message/entryKey 字段，新增 type 字段。

```java
package top.enderliquid.audioflow.common.annotation;

import top.enderliquid.audioflow.common.enums.LimitType;

import java.lang.annotation.*;

/**
 * 单一限流规则注解 - 只能在 @RateLimits 容器注解内使用
 */
@Documented
@Target({})  // 空目标，表示只能在容器注解内使用
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {
    /**
     * 限流类型：IP / USER / GLOBAL
     */
    LimitType type();
    
    /**
     * 令牌补充速率，格式为 "分子/分母"，表示每分母秒补充分子个令牌
     * 例如 "5/1" 表示每秒补充 5 个令牌
     */
    String refillRate() default "5/1";
    
    /**
     * 令牌桶容量
     */
    int capacity() default 10;
    
    /**
     * 每次请求消耗的令牌数
     */
    int tokensRequested() default 1;
}
```

**Step 2: 验证注解修改正确**

Run: `./mvnw compile -DskipTests`
Expected: BUILD SUCCESS（此时会有编译错误，正常，因为 Controller 还在使用旧注解格式）

**Step 3: 提交**

```bash
git add src/main/java/top/enderliquid/audioflow/common/annotation/RateLimit.java
git commit -m "重构 RateLimit 注解为单一规则注解"
```

---

## Task 4: 修改 RateLimitService 接口

**Files:**
- Modify: `src/main/java/top/enderliquid/audioflow/service/RateLimitService.java`

**Step 1: 修改方法签名**

将 entryKey 参数改名为 entry，添加 message 参数，RateLimit 参数改为单一规则注解。

```java
package top.enderliquid.audioflow.service;

import jakarta.validation.constraints.NotNull;
import top.enderliquid.audioflow.common.annotation.RateLimit;

public interface RateLimitService {
    void verifyRateLimit(@NotNull RateLimit limit, @NotNull String ip, Long userId, @NotNull String entry, @NotNull String message);
}
```

**Step 2: 验证接口修改正确**

Run: `./mvnw compile -DskipTests`
Expected: BUILD SUCCESS

**Step 3: 提交**

```bash
git add src/main/java/top/enderliquid/audioflow/service/RateLimitService.java
git commit -m "修改 RateLimitService 接口方法签名"
```

---

## Task 5: 修改 RateLimitServiceImpl 实现

**Files:**
- Modify: `src/main/java/top/enderliquid/audioflow/service/impl/RateLimitServiceImpl.java`

**Step 1: 重构验证逻辑**

支持 GLOBAL 类型，修复 Redis key 生成逻辑，重构为单一规则验证。

```java
package top.enderliquid.audioflow.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import top.enderliquid.audioflow.common.annotation.RateLimit;
import top.enderliquid.audioflow.common.enums.LimitType;
import top.enderliquid.audioflow.common.exception.RateLimitException;
import top.enderliquid.audioflow.common.util.Fraction;
import top.enderliquid.audioflow.manager.RedisManager;
import top.enderliquid.audioflow.service.RateLimitService;

@Slf4j
@Service
public class RateLimitServiceImpl implements RateLimitService {
    @Autowired
    private RedisManager redisManager;

    @Override
    public void verifyRateLimit(RateLimit limit, String ip, Long userId, String entry, String message) {
        double refillRate = new Fraction(limit.refillRate()).toDouble();
        int capacity = limit.capacity();
        LimitType limitType = limit.type();
        int tokensRequested = limit.tokensRequested();

        log.debug("验证限流，IP: {}, 用户ID: {}, 入口: {}, 类型: {}", ip, userId, entry, limitType);

        String key;
        switch (limitType) {
            case IP:
                key = generateIpKey(ip, entry);
                break;
            case USER:
                if (userId == null) {
                    log.debug("用户未登录，跳过用户限流检查，入口: {}", entry);
                    return;
                }
                key = generateUserIdKey(userId, entry);
                break;
            case GLOBAL:
                key = generateGlobalKey(entry);
                break;
            default:
                throw new IllegalArgumentException("未知的限流类型: " + limitType);
        }

        if (!redisManager.tryAcquireRateLimitToken(key, capacity, refillRate, tokensRequested)) {
            log.warn("{}限流检查失败，入口: {}, 容量: {}, 补充速率: {}, 需求量: {}", 
                limitType, entry, capacity, refillRate, tokensRequested);
            throw new RateLimitException(message);
        }

        log.debug("限流验证通过，IP: {}, 用户ID: {}, 入口: {}, 类型: {}", ip, userId, entry, limitType);
    }

    private String generateIpKey(String ip, String entry) {
        return "rate_limit:" +
                "ip-" + ip +
                ";entry-" + entry;
    }

    private String generateUserIdKey(Long userId, String entry) {
        return "rate_limit:" +
                "user-" + userId +
                ";entry-" + entry;
    }

    private String generateGlobalKey(String entry) {
        return "rate_limit:" +
                "global;entry-" + entry;
    }
}
```

**Step 2: 验证实现修改正确**

Run: `./mvnw compile -DskipTests`
Expected: BUILD SUCCESS

**Step 3: 提交**

```bash
git add src/main/java/top/enderliquid/audioflow/service/impl/RateLimitServiceImpl.java
git commit -m "重构 RateLimitServiceImpl 支持 GLOBAL 类型"
```

---

## Task 6: 修改 RateLimitAspect 切面

**Files:**
- Modify: `src/main/java/top/enderliquid/audioflow/common/config/RateLimitAspect.java`

**Step 1: 重构 Aspect 支持 RateLimits 容器注解**

```java
package top.enderliquid.audioflow.common.config;

import cn.dev33.satoken.stp.StpUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import top.enderliquid.audioflow.common.annotation.RateLimit;
import top.enderliquid.audioflow.common.annotation.RateLimits;
import top.enderliquid.audioflow.service.RateLimitService;

@Slf4j
@Component
@Aspect
public class RateLimitAspect {

    @Autowired
    private RateLimitService rateLimitService;

    @Around("@annotation(rateLimits)")
    public Object applyRateLimit(ProceedingJoinPoint joinPoint, RateLimits rateLimits) throws Throwable {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            throw new RuntimeException("请求属性为空");
        }
        HttpServletRequest request = attributes.getRequest();
        String ip = getClientIp(request);
        Long userId = getUserId();
        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        String methodName = methodSignature.getMethod().getName();
        String entry = rateLimits.entry().isEmpty() ? methodName : rateLimits.entry();

        // 遍历所有限流规则，依次验证
        for (RateLimit limit : rateLimits.value()) {
            rateLimitService.verifyRateLimit(limit, ip, userId, entry, rateLimits.message());
        }

        return joinPoint.proceed();
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
}
```

**Step 2: 验证 Aspect 修改正确**

Run: `./mvnw compile -DskipTests`
Expected: BUILD SUCCESS

**Step 3: 提交**

```bash
git add src/main/java/top/enderliquid/audioflow/common/config/RateLimitAspect.java
git commit -m "重构 RateLimitAspect 支持 RateLimits 容器注解"
```

---

## Task 7: 迁移 SessionController

**Files:**
- Modify: `src/main/java/top/enderliquid/audioflow/controller/SessionController.java`

**Step 1: 迁移 SessionController 中的 @RateLimit 注解**

将所有 `@RateLimit` 改为 `@RateLimits` + `@RateLimit(type = ...)` 格式。

根据原注解配置迁移：
- `limitType = LimitType.IP` → `@RateLimit(type = LimitType.IP)`
- `limitType = LimitType.USER` → `@RateLimit(type = LimitType.USER)`
- `limitType = LimitType.BOTH` → `@RateLimit(type = LimitType.IP), @RateLimit(type = LimitType.USER)`
- `entryKey = "xxx"` → `entry = "xxx"`

先读取文件查看具体内容，再进行修改。

**Step 2: 验证迁移正确**

Run: `./mvnw compile -DskipTests`
Expected: BUILD SUCCESS

**Step 3: 提交**

```bash
git add src/main/java/top/enderliquid/audioflow/controller/SessionController.java
git commit -m "迁移 SessionController 到新限流注解格式"
```

---

## Task 8: 迁移 UserController

**Files:**
- Modify: `src/main/java/top/enderliquid/audioflow/controller/UserController.java`

**Step 1: 迁移 UserController 中的 @RateLimit 注解**

先读取文件查看具体内容，再根据迁移映射规则进行修改。

**Step 2: 验证迁移正确**

Run: `./mvnw compile -DskipTests`
Expected: BUILD SUCCESS

**Step 3: 提交**

```bash
git add src/main/java/top/enderliquid/audioflow/controller/UserController.java
git commit -m "迁移 UserController 到新限流注解格式"
```

---

## Task 9: 迁移 SongController

**Files:**
- Modify: `src/main/java/top/enderliquid/audioflow/controller/SongController.java`

**Step 1: 迁移 SongController 中的 @RateLimit 注解**

先读取文件查看具体内容，再根据迁移映射规则进行修改。

**Step 2: 验证迁移正确**

Run: `./mvnw compile -DskipTests`
Expected: BUILD SUCCESS

**Step 3: 提交**

```bash
git add src/main/java/top/enderliquid/audioflow/controller/SongController.java
git commit -m "迁移 SongController 到新限流注解格式"
```

---

## Task 10: 更新集成测试

**Files:**
- Modify: `src/test/java/top/enderliquid/audioflow/common/config/RateLimitAspectIntegrationTest.java`

**Step 1: 更新测试以适应新注解格式**

测试应该使用新的 `@RateLimits` 注解格式。根据测试逻辑，可能需要添加新的测试用例验证多限流维度组合和全局限流功能。

先读取测试文件，评估需要修改的内容。

**Step 2: 运行测试验证功能**

Run: `./mvnw test -Dtest=RateLimitAspectIntegrationTest`
Expected: Tests run successfully

**Step 3: 提交**

```bash
git add src/test/java/top/enderliquid/audioflow/common/config/RateLimitAspectIntegrationTest.java
git commit -m "更新限流集成测试以适应新注解格式"
```

---

## Task 11: 运行所有测试验证

**Files:**
- 无文件修改，仅运行测试

**Step 1: 运行所有测试**

Run: `./mvnw clean test`
Expected: All tests pass

**Step 2: 提交验证结果**

如果测试全部通过，无需提交，继续下一步。如果测试失败，修复问题并提交。

---

## Task 12: 清理无用导入

**Files:**
- 可能涉及多个 Controller 文件

**Step 1: 检查并清理无用导入**

移除 Controller 文件中不再使用的 `top.enderliquid.audioflow.common.enums.LimitType` 导入（如果在新注解格式中不再直接使用 LimitType）。

Run: `./mvnw compile -DskipTests`
Expected: BUILD SUCCESS

**Step 2: 提交清理结果**

```bash
git add -A
git commit -m "清理无用导入"
```

---

## 执行顺序总结

1. Task 1: 修改 LimitType 枚举
2. Task 2: 创建 RateLimits 容器注解
3. Task 3: 修改 RateLimit 注解
4. Task 4: 修改 RateLimitService 接口
5. Task 5: 修改 RateLimitServiceImpl 实现
6. Task 6: 修改 RateLimitAspect 切面
7. Task 7: 迁移 SessionController
8. Task 8: 迁移 UserController
9. Task 9: 迁移 SongController
10. Task 10: 更新集成测试
11. Task 11: 运行所有测试验证
12. Task 12: 清理无用导入

**注意**：Task 1-6 必须顺序执行，因为存在依赖关系。Task 7-9 可以并行执行（如果使用 subagent-driven-development）。Task 10-12 必须在所有迁移完成后执行。