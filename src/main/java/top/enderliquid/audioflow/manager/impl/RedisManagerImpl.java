package top.enderliquid.audioflow.manager.impl;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;
import top.enderliquid.audioflow.manager.RedisManager;

import java.io.IOException;
import java.util.Collections;

@Slf4j
@Component
@Validated
public class RedisManagerImpl implements RedisManager {

    @Autowired
    private StringRedisTemplate redisTemplate;

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

    public boolean tryAcquireRateLimitToken(String key, int capacity, double refillRate, int tokensRequested) {
        long now = System.currentTimeMillis() / 1000;
        Long result = redisTemplate.execute(
            rateLimitScript,
            Collections.singletonList(key),
            String.valueOf(capacity),
            String.valueOf(refillRate),
            String.valueOf(now),
            String.valueOf(tokensRequested)
        );
        return result != -1;
    }
}
