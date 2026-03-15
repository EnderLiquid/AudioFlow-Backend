package top.enderliquid.audioflow.manager;

public interface RateLimitManager {
    boolean tryAcquireRateLimitToken(String key, int capacity, double refillRate, int tokensRequested);
}
