package top.enderliquid.audioflow.service;

import jakarta.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;
import top.enderliquid.audioflow.common.annotation.RateLimit;

@Validated
public interface RateLimitService {
    void verifyRateLimit(@NotNull RateLimit rateLimit, @NotNull String ip, Long userId, @NotNull String methodName);
}
