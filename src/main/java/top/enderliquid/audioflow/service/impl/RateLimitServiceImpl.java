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
            case IP -> key = generateIpKey(ip, entry);
            case USER -> {
                if (userId == null) {
                    log.debug("用户未登录，跳过用户限流检查，入口: {}", entry);
                    return;
                }
                key = generateUserIdKey(userId, entry);
            }
            case GLOBAL -> key = generateGlobalKey(entry);
            default -> throw new IllegalArgumentException("未知的限流类型: " + limitType);
        }

        if (!redisManager.tryAcquireRateLimitToken(key, capacity, refillRate, tokensRequested)) {
            log.warn("限流检查失败，类型: {}，入口: {}, 容量: {}, 补充速率: {}, 需求量: {}",
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