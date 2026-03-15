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
import top.enderliquid.audioflow.manager.CheckinCountManager;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Objects;

/**
 * 日登录数统计管理器实现
 * 使用Redis String自增进行日登录数的精确统计
 * 键格式: checkin_count:yyyy-M-d（如 checkin_count:2026-3-15）
 * 过期时间: 30天
 */
@Slf4j
@Component
public class CheckinCountManagerImpl implements CheckinCountManager {

    @Autowired
    private StringRedisTemplate redisTemplate;

    private RedisScript<Long> checkinCountScript;

    private static final String KEY_PREFIX = "checkin_count:";
    private static final long EXPIRE_DAYS = 30;
    private static final long EXPIRE_SECONDS = EXPIRE_DAYS * 24 * 60 * 60;

    @PostConstruct
    public void init() {
        Resource resource = new ClassPathResource("scripts/checkin_count.lua");
        try {
            String script = new String(resource.getContentAsByteArray());
            checkinCountScript = new DefaultRedisScript<>(script, Long.class);
        } catch (IOException e) {
            log.error("加载登录计数Lua脚本失败", e);
            throw new RuntimeException("加载登录计数Lua脚本失败", e);
        }
    }

    @Override
    public long incrementCheckinCount(LocalDate date) {
        String key = buildKey(date);
        // 使用Lua脚本原子递增并设置过期时间
        try {
            Long count = redisTemplate.execute(
                    checkinCountScript,
                    Collections.singletonList(key),
                    String.valueOf(EXPIRE_SECONDS)
            );
            Objects.requireNonNull(count);
            log.debug("递增日登录计数成功，日期: {}, 当前计数: {}", date, count);
            return count;
        } catch (Exception e) {
            log.error("递增日登录计数失败，日期: {}", date, e);
            return 0L;
        }
    }

    @Override
    public long getCheckinCount(LocalDate date) {
        String key = buildKey(date);
        String value = redisTemplate.opsForValue().get(key);
        if (value == null) {
            return 0L;
        }
        // 防御性编程：处理数字解析异常
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            log.warn("登录计数值格式异常，键: {}, 值: {}", key, value);
            return 0L;
        }
    }

    /**
     * 构建Redis键
     *
     * @param date 日期
     * @return 键字符串，格式为 checkin_count:yyyy-M-d
     */
    private String buildKey(LocalDate date) {
        return KEY_PREFIX + date.getYear() + "-" + date.getMonthValue() + "-" + date.getDayOfMonth();
    }
}