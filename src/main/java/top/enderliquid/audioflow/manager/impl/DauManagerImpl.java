package top.enderliquid.audioflow.manager.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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
import top.enderliquid.audioflow.entity.Dau;
import top.enderliquid.audioflow.manager.DauManager;
import top.enderliquid.audioflow.mapper.DauMapper;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collections;
import java.util.Objects;

import static top.enderliquid.audioflow.common.constant.DefaultConstants.DAILY_STATS_REDIS_EXPIRE_DAYS;

/**
 * 日活统计管理器实现
 * 使用Redis HyperLogLog进行日活的粗略统计
 * 键格式: dau:yyyy-M-d（如 dau:2026-3-15）
 * 同时继承ServiceImpl管理数据库操作
 */
@Slf4j
@Component
public class DauManagerImpl extends ServiceImpl<DauMapper, Dau> implements DauManager {

    @Autowired
    private StringRedisTemplate redisTemplate;

    private RedisScript<Long> dauAddScript;

    private static final String KEY_PREFIX = "dau:";
    private static final long EXPIRE_SECONDS = DAILY_STATS_REDIS_EXPIRE_DAYS * 24 * 60 * 60;

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
    public void addRecord(Long userId, LocalDate date) {
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
                log.debug("添加日活记录成功，用户ID: {}, 日期: {}", userId, date);
            }
        } catch (Exception e) {
            log.error("添加日活记录失败，用户ID: {}, 日期: {}", userId, date, e);
        }
    }

    @Override
    public long getDauCount(LocalDate date) {
        String key = buildKey(date);
        Long count = redisTemplate.opsForHyperLogLog().size(key);
        return count != null ? count : 0L;
    }

    @Override
    public int syncAllFromRedis() {
        LocalDate startDate = LocalDate.now().minusDays(DAILY_STATS_REDIS_EXPIRE_DAYS - 1);
        LocalDate today = LocalDate.now();
        int syncCount = 0;

        for (LocalDate date = startDate; !date.isAfter(today); date = date.plusDays(1)) {
            long count = getDauCount(date);
            if (count == 0) {
                continue;
            }

            // 查询数据库中是否已存在该日期的记录
            LambdaQueryWrapper<Dau> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(Dau::getDate, date);
            Dau existing = getOne(queryWrapper);

            if (existing == null) {
                // 记录不存在，尝试插入
                try {
                    Dau dau = new Dau();
                    dau.setDate(date);
                    dau.setCount(count);
                    save(dau);
                    log.debug("插入日活数据成功，日期: {}, 日活数: {}", date, count);
                    syncCount++;
                } catch (DuplicateKeyException e) {
                    log.warn("插入日活数据时发生唯一键冲突，可能存在并发写入，日期: {}", date);
                }
            } else {
                // 记录存在，检查updateTime是否已写入最终数据
                LocalDateTime dayEndTime = date.atTime(LocalTime.MAX);
                if (existing.getUpdateTime() != null && !existing.getUpdateTime().isAfter(dayEndTime)) {
                    // updateTime不晚于当天结束时间，需要更新
                    existing.setCount(count);
                    updateById(existing);
                    log.debug("更新日活数据成功，日期: {}, 日活数: {}", date, count);
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
     * @return 键字符串，格式为 dau:yyyy-M-d
     */
    private String buildKey(LocalDate date) {
        return KEY_PREFIX + date.getYear() + "-" + date.getMonthValue() + "-" + date.getDayOfMonth();
    }
}