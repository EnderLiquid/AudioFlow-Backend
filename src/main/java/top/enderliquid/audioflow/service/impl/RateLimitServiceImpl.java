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
        log.debug("验证限流，限流规则: {}, IP: {}, 用户ID: {}, 方法: {}", rateLimit.refillRate(), ip, userId, methodName);
        double refillRate = new Fraction(rateLimit.refillRate()).toDouble();
        int capacity = rateLimit.capacity();
        LimitType limitType = rateLimit.limitType();

        if (limitType == LimitType.BOTH || limitType == LimitType.IP) {
            String ipKey = generateIpKey(ip, methodName);
            if (!redisManager.tryAcquireRateLimitToken(ipKey, capacity, refillRate, 1)) {
                log.warn("IP限流检查失败，IP: {}, 方法: {}, 容量: {}, 补充速率: {}", ip, methodName, capacity, refillRate);
                throw new RateLimitException(rateLimit.message());
            }
        }

        if (userId != null && (limitType == LimitType.BOTH || limitType == LimitType.USER)) {
            String userIdKey = generateUserIdKey(userId, methodName);
            if (!redisManager.tryAcquireRateLimitToken(userIdKey, capacity, refillRate, 1)) {
                log.warn("用户限流检查失败，用户ID: {}, 方法: {}, 容量: {}, 补充速率: {}", userId, methodName, capacity, refillRate);
                throw new RateLimitException(rateLimit.message());
            }
        }

        log.debug("限流验证通过，IP: {}, 用户ID: {}, 方法: {}", ip, userId, methodName);
    }

    private String generateIpKey(String ip, String methodName) {
        return "rate_limit:" +
                "ip-" + ip +
                ";method-" + methodName;
    }

    private String generateUserIdKey(Long userId, String methodName) {
        return "rate_limit:" +
                "ip-" + userId +
                ";method-" + methodName;
    }
}
