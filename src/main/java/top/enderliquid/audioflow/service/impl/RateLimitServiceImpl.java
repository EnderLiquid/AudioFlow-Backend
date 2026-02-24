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
    public void verifyRateLimit(RateLimit rateLimit, String ip, Long userId, String methodName) {
        double refillRate = new Fraction(rateLimit.refillRate()).toDouble();
        int capacity = rateLimit.capacity();
        LimitType limitType = rateLimit.limitType();
        int tokensRequested = rateLimit.tokensRequested();

        String entryKey;
        if (rateLimit.entryKey().isEmpty()) {
            entryKey = methodName;
        } else {
            entryKey = rateLimit.entryKey();
        }

        log.debug("验证限流，IP: {}, 用户ID: {}, 入口: {}", ip, userId, entryKey);

        if (limitType == LimitType.BOTH || limitType == LimitType.IP) {
            String ipKey = generateIpKey(ip, entryKey);
            if (!redisManager.tryAcquireRateLimitToken(ipKey, capacity, refillRate, tokensRequested)) {
                log.warn("IP限流检查失败，IP: {}, 入口: {}, 容量: {}, 补充速率: {}, 需求量: {}", ip, entryKey, capacity, refillRate, tokensRequested);
                throw new RateLimitException(rateLimit.message());
            }
        }

        if (userId != null && (limitType == LimitType.BOTH || limitType == LimitType.USER)) {
            String userIdKey = generateUserIdKey(userId, entryKey);
            if (!redisManager.tryAcquireRateLimitToken(userIdKey, capacity, refillRate, tokensRequested)) {
                log.warn("用户限流检查失败，用户ID: {}, 入口: {}, 容量: {}, 补充速率: {}, 需求量: {}", userId, entryKey, capacity, refillRate, tokensRequested);
                throw new RateLimitException(rateLimit.message());
            }
        }

        log.debug("限流验证通过，IP: {}, 用户ID: {}, 入口: {}", ip, userId, entryKey);
    }

    private String generateIpKey(String ip, String entryKey) {
        return "rate_limit:" +
                "ip-" + ip +
                ";entry-" + entryKey;
    }

    private String generateUserIdKey(Long userId, String entryKey) {
        return "rate_limit:" +
                "ip-" + userId +
                ";entry-" + entryKey;
    }
}
