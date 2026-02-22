package top.enderliquid.audioflow.common.annotation;

import top.enderliquid.audioflow.common.enums.LimitType;

import java.lang.annotation.*;

/**
 * 限流注解 - 基于令牌桶算法的API限流配置
 */
@Documented
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {
    String refillRate() default "5/1";

    int capacity() default 10;

    LimitType limitType() default LimitType.BOTH;

    String message() default "请求过于频繁，请稍后再试";
}
