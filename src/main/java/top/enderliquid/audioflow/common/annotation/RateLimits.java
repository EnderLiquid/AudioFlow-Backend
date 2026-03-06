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