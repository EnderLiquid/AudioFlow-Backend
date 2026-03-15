package top.enderliquid.audioflow.manager.impl;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;
import top.enderliquid.audioflow.manager.DauManager;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Objects;

/**
 * 日活统计管理器实现
 * 使用Redis HyperLogLog进行日活的粗略统计
 * 键格式: dau:yyyy-M-d（如 dau:2026-3-15）
 * 过期时间: 30天
 */
@Slf4j
@Component
public class DauManagerImpl implements DauManager {

    @Autowired
    private StringRedisTemplate redisTemplate;

    private RedisScript<Long> dauAddScript;

    private static final String KEY_PREFIX = "dau:";
    private static final long EXPIRE_DAYS = 30;
    private static final long EXPIRE_SECONDS = EXPIRE_DAYS * 24 * 60 * 60;

    @PostConstruct
    public void init() {
        Resource resource = new ClassPathResource("scripts/dau_add.lua");
        try {
            String script = new String(resource.getContentAsByteArray());
            dauAddScript = new DefaultRedisScript<>(script, Long.class);
        } catch (IOException e) {
            log.error("加载DAU Lua脚本失败", e);
            throw new RuntimeException("加载DAU Lua脚本失败", e);
        }
    }

    @Override
    public void recordDau(Long userId, LocalDate date) {
        String key = buildKey(date);
        String element = String.valueOf(userId);

        // 使用Lua脚本原子添加元素到HyperLogLog并设置过期时间
        try {
            Long added = redisTemplate.execute(
                    dauAddScript,
                    Collections.singletonList(key),
                    element,
                    String.valueOf(EXPIRE_SECONDS)
            );
            Objects.requireNonNull(added);
            if (added > 0) {
                log.debug("记录日活成功，用户ID: {}, 日期: {}", userId, date);
            }
        } catch (Exception e) {
            log.error("记录日活失败，用户ID: {}, 日期: {}", userId, date, e);
        }
    }

    @Override
    public long getDauCount(LocalDate date) {
        String key = buildKey(date);
        Long count = redisTemplate.opsForHyperLogLog().size(key);
        return count != null ? count : 0L;
    }

    /**
     * 构建Redis键
     *
     * @param date 日期
     * @return 键字符串，格式为 dau:yyyy-M-d
     */
    private String buildKey(LocalDate date) {
        return KEY_PREFIX + date.getYear() + "-" + date.getMonthValue() + "-" + date.getDayOfMonth();
    }
}