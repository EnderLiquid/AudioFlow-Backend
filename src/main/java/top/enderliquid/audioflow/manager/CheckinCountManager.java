package top.enderliquid.audioflow.manager;

import org.springframework.validation.annotation.Validated;

import java.time.LocalDate;

/**
 * 日签到数统计管理器
 * 使用Redis String自增进行日签到数的精确统计
 */
@Validated
public interface CheckinCountManager {

    /**
     * 递增指定日期的签到计数
     *
     * @param date 日期
     * @return 递增后的计数值
     */
    long incrementCheckinCount(LocalDate date);

    /**
     * 获取指定日期的签到计数
     *
     * @param date 日期
     * @return 签到计数，如果键不存在返回0
     */
    long getCheckinCount(LocalDate date);
}