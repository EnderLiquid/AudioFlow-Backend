package top.enderliquid.audioflow.manager.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;
import top.enderliquid.audioflow.entity.CheckinCount;
import top.enderliquid.audioflow.manager.CheckinCountManager;
import top.enderliquid.audioflow.mapper.CheckinCountMapper;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collections;
import java.util.Objects;

import static top.enderliquid.audioflow.common.constant.DefaultConstants.DAILY_STATS_REDIS_EXPIRE_DAYS;

/**
 * 日签到数统计管理器实现
 * 使用Redis String自增进行日签到数的精确统计
 * 键格式: checkin_count:yyyy-M-d（如 checkin_count:2026-3-15）
 * 同时继承ServiceImpl管理数据库操作
 */
@Slf4j
@Component
public class CheckinCountManagerImpl extends ServiceImpl<CheckinCountMapper, CheckinCount> implements CheckinCountManager {

    @Autowired
    private StringRedisTemplate redisTemplate;

    private RedisScript<Long> checkinCountScript;

    private static final String KEY_PREFIX = "checkin_count:";
    private static final long EXPIRE_SECONDS = DAILY_STATS_REDIS_EXPIRE_DAYS * 24 * 60 * 60;

    @PostConstruct
    public void init() {
        Resource resource = new ClassPathResource("scripts/checkin_count.lua");
        try {
            String script = new String(resource.getContentAsByteArray());
            checkinCountScript = new DefaultRedisScript<>(script, Long.class);
        } catch (IOException e) {
            log.error("加载签到计数Lua脚本失败", e);
            throw new RuntimeException("加载签到计数Lua脚本失败", e);
        }
    }

    @Override
    public long addRecord(LocalDate date) {
        String key = buildKey(date);
        // 使用Lua脚本原子递增并设置过期时间
        try {
            Long count = redisTemplate.execute(
                    checkinCountScript,
                    Collections.singletonList(key),
                    String.valueOf(EXPIRE_SECONDS)
            );
            Objects.requireNonNull(count);
            log.debug("添加签到计数记录成功，日期: {}, 当前计数: {}", date, count);
            return count;
        } catch (Exception e) {
            log.error("添加签到计数记录失败，日期: {}", date, e);
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
            log.warn("签到计数值格式异常，键: {}, 值: {}", key, value);
            return 0L;
        }
    }

    @Override
    public int syncAllFromRedis() {
        LocalDate startDate = LocalDate.now().minusDays(DAILY_STATS_REDIS_EXPIRE_DAYS - 1);
        LocalDate today = LocalDate.now();
        int syncCount = 0;

        for (LocalDate date = startDate; !date.isAfter(today); date = date.plusDays(1)) {
            long count = getCheckinCount(date);
            if (count == 0) {
                continue;
            }

            // 查询数据库中是否已存在该日期的记录
            CheckinCount existing = lambdaQuery()
                    .eq(CheckinCount::getDate, date)
                    .one();

            if (existing == null) {
                // 记录不存在，尝试插入
                try {
                    CheckinCount checkinCount = new CheckinCount();
                    checkinCount.setDate(date);
                    checkinCount.setCount(count);
                    save(checkinCount);
                    log.debug("插入签到数据成功，日期: {}, 签到数: {}", date, count);
                    syncCount++;
                } catch (DuplicateKeyException e) {
                    log.warn("插入签到数据时发生唯一键冲突，可能存在并发写入，日期: {}", date);
                }
            } else {
                // 记录存在，检查updateTime是否已写入最终数据
                LocalDateTime dayEndTime = date.atTime(LocalTime.MAX);
                if (existing.getUpdateTime() != null && !existing.getUpdateTime().isAfter(dayEndTime)) {
                    // updateTime不晚于当天结束时间，需要更新
                    existing.setCount(count);
                    updateById(existing);
                    log.debug("更新签到数据成功，日期: {}, 签到数: {}", date, count);
                    syncCount++;
                }
            }
        }
        return syncCount;
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