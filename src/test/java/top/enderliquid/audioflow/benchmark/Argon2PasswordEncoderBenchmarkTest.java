package top.enderliquid.audioflow.benchmark;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;

/**
 * Argon2密码编码器性能基准测试
 */
@SpringBootTest
class Argon2PasswordEncoderBenchmarkTest {

    @Test
    public void benchmarkArgon2EncodingPerformance() {
        Argon2PasswordEncoder encoder = new Argon2PasswordEncoder(16, 32, 1, 16384, 3);
        String rawPassword = "my_password_123";

        long startTime = System.currentTimeMillis();
        for (int i = 0; i < 10; i++) {
            encoder.encode(rawPassword);
        }
        long endTime = System.currentTimeMillis();

        long averageTime = (endTime - startTime) / 10;
        String encodedPassword = encoder.encode(rawPassword);

        System.out.println("Argon2编码平均耗时: " + averageTime + "ms");
        System.out.println("Argon2密文长度: " + encodedPassword.length());
    }
}
