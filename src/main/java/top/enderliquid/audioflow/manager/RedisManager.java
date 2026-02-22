package top.enderliquid.audioflow.manager;

public interface RedisManager {
    boolean tryAcquireRateLimitToken(String key, int capacity, double refillRate, int tokensRequested);
}
