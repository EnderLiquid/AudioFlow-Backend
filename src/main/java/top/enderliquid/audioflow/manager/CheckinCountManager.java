package top.enderliquid.audioflow.manager;

import org.springframework.validation.annotation.Validated;

import java.time.LocalDate;

/**
 * 日登录数统计管理器
 * 使用Redis String自增进行日登录数的精确统计
 */
@Validated
public interface CheckinCountManager {

    /**
     * 递增指定日期的登录计数
     *
     * @param date 日期
     * @return 递增后的计数值
     */
    long incrementCheckinCount(LocalDate date);

    /**
     * 获取指定日期的登录计数
     *
     * @param date 日期
     * @return 登录计数，如果键不存在返回0
     */
    long getCheckinCount(LocalDate date);
}