package top.enderliquid.audioflow.common.annotation;

import top.enderliquid.audioflow.common.enums.LimitType;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 限流注解 - 基于令牌桶算法的API限流配置
 */
@Documented
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {
    String refillRate();

    String capacity();

    LimitType limitType() default LimitType.BOTH;

    String message() default "请求过于频繁，请稍后再试";
}
