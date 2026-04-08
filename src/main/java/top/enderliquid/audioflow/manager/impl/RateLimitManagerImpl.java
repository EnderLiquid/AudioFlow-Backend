package top.enderliquid.audioflow.manager.impl;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;
import top.enderliquid.audioflow.manager.RateLimitManager;

import java.io.IOException;
import java.util.Collections;
import java.util.Objects;

@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitManagerImpl implements RateLimitManager {

    private final StringRedisTemplate redisTemplate;

    private RedisScript<Long> rateLimitScript;

    @PostConstruct
    public void init() {
        Resource resource = new ClassPathResource("scripts/rate_limit.lua");
        try {
            String script = new String(resource.getContentAsByteArray());
            rateLimitScript = new DefaultRedisScript<>(script, Long.class);
        } catch (IOException e) {
            log.error("加载Lua脚本失败", e);
            throw new RuntimeException("加载Lua脚本失败", e);
        }
    }

    /**
     * 尝试基于令牌桶算法获取限流令牌。
     *
     * @param key             限流维度的唯一标识
     * @param capacity        令牌桶的最大容量（允许的最大突发请求数）
     * @param refillRate      令牌补充速率（单位：个/秒，支持浮点数，如 0.5 表示每 2 秒补充 1 个）
     * @param tokensRequested 本次请求需要消耗的令牌数量（通常为 1）
     * @return {@code true} 如果获取令牌成功（请求被允许）；
     * {@code false} 如果令牌不足（请求被限流）。
     */
    public boolean tryAcquireRateLimitToken(String key, int capacity, double refillRate, int tokensRequested) {
        try {
            Long result = redisTemplate.execute(
                    rateLimitScript,
                    Collections.singletonList(key),
                    String.valueOf(capacity),
                    String.valueOf(refillRate),
                    String.valueOf(System.currentTimeMillis()),
                    String.valueOf(tokensRequested)
            );
            Objects.requireNonNull(result);
            return result != -1;
        } catch (Exception e) {
            log.error("Redis限流脚本执行异常，默认放行。Key: {}", key, e);
            return true;
        }
    }
}
