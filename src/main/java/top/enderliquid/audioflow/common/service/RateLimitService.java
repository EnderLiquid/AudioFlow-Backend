package top.enderliquid.audioflow.common.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import top.enderliquid.audioflow.common.enums.LimitType;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.util.Collections;

@Slf4j
@Service
@Validated
public class RateLimitService {

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
