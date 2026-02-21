package top.enderliquid.audioflow.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 限流配置属性类
 */
@Data
@Component
@ConfigurationProperties(prefix = "rate-limit")
public class RateLimitProperties {
    private DefaultConfig defaultConfig = new DefaultConfig();

    @Data
    public static class DefaultConfig {
        private String refillRate = "5/1";
        private String capacity = "10";
    }
}
