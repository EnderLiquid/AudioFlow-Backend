package top.enderliquid.audioflow.common.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import top.enderliquid.audioflow.common.enums.LimitType;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class RateLimitServiceTest {

    @InjectMocks
    private RateLimitService rateLimitService;

    @Test
    void testParseRefillRate_Success() {
        double rate = rateLimitService.parseRefillRate("5/1");
        assertEquals(5.0, rate);
    }

    @Test
    void testParseRefillRate_PerMinute() {
        double rate = rateLimitService.parseRefillRate("3/60");
        assertEquals(0.05, rate, 0.001);
    }

    @Test
    void testParseRefillRate_InvalidFormat() {
        assertThrows(IllegalArgumentException.class, () -> {
            rateLimitService.parseRefillRate("invalid");
        });
    }

    @Test
    void testGenerateKey_IP() {
        String key = rateLimitService.generateKey("192.168.1.1", null, "/api/sessions", LimitType.IP);
        assertEquals("rate_limit:ip:192.168.1.1:/api/sessions", key);
    }

    @Test
    void testGenerateKey_USER() {
        String key = rateLimitService.generateKey("192.168.1.1", 123L, "/api/songs", LimitType.USER);
        assertEquals("rate_limit:user:123:/api/songs", key);
    }

    @Test
    void testGenerateKey_USER_NotLogin() {
        assertThrows(IllegalArgumentException.class, () -> {
            rateLimitService.generateKey("192.168.1.1", null, "/api/songs", LimitType.USER);
        });
    }

    @Test
    void testGenerateKey_BOTH() {
        String key = rateLimitService.generateKey("192.168.1.1", 123L, "/api/sessions", LimitType.BOTH);
        assertEquals("rate_limit:both:192.168.1.1:123:/api/sessions", key);
    }
}
