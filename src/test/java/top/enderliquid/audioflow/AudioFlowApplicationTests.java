package top.enderliquid.audioflow;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;

@SpringBootTest
class AudioFlowApplicationTests {
    @Test
    public void benchmarkArgon2() {
        Argon2PasswordEncoder encoder = new Argon2PasswordEncoder(16, 32, 1, 16384, 3);
        String raw = "my_password_123";
        long start = System.currentTimeMillis();
        for (int i = 0; i < 10; i++) {
            encoder.encode(raw);
        }
        long end = System.currentTimeMillis();
        System.out.println("平均耗时: " + (end - start) / 10 + "ms");
        System.out.println("密文长度: " + encoder.encode(raw).length());
    }
}
