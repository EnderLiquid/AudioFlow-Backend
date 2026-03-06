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