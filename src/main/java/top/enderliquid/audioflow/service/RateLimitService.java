package top.enderliquid.audioflow.service;

import jakarta.validation.constraints.NotNull;
import top.enderliquid.audioflow.common.annotation.RateLimit;

public interface RateLimitService {
    void verifyRateLimit(@NotNull RateLimit limit, @NotNull String ip, Long userId, @NotNull String entry, @NotNull String message);
}