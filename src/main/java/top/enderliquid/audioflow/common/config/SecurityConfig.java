package top.enderliquid.audioflow.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class SecurityConfig {

    @Value("${password.encrypt.bcrypt.work-factor:12}")
    private int bcryptWorkFactor;

    @Value("${password.encrypt.argon2.parallelism:1}")
    private int argon2Parallelism;

    @Value("${password.encrypt.argon2.memory:16384}")
    private int argon2Memory;

    @Value("${password.encrypt.argon2.iterations:3}")
    private int argon2Iterations;

    // 新建密码策略
    @Value("${password.encrypt.id-for-encode:argon2}")
    private String idForEncode;

    @Bean
    public PasswordEncoder passwordEncoder() {
        Map<String, PasswordEncoder> encoders = new HashMap<>();
        // Bcrypt (密码>72字节会被截断)
        encoders.put("bcrypt", new BCryptPasswordEncoder(bcryptWorkFactor));
        // Argon2
        encoders.put("argon2", new Argon2PasswordEncoder(
                16,
                32,
                argon2Parallelism,
                argon2Memory,
                argon2Iterations
        ));
        return new DelegatingPasswordEncoder(idForEncode, encoders);
    }
}

