package top.enderliquid.audioflow.common.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class RateLimitLuaScriptTest {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Test
    void testLuaScriptExecution() {
        String key = "test_rate_limit";

        long result = redisTemplate.execute(
            new org.springframework.data.redis.core.script.DefaultRedisScript<>(
                "return 1",
                Long.class
            ),
            java.util.Collections.singletonList(key)
        );

        assertEquals(1L, result);
    }
}
