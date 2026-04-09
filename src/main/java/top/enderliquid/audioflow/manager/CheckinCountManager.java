package top.enderliquid.audioflow.manager;

import com.baomidou.mybatisplus.extension.service.IService;
import top.enderliquid.audioflow.entity.CheckinCount;

import java.time.LocalDate;

/**
 * 日签到数统计管理器
 * 使用Redis String自增进行日签到数的精确统计
 * 同时管理Redis缓存和MySQL持久化
 */
public interface CheckinCountManager extends IService<CheckinCount> {

    /**
     * 添加签到计数记录（递增指定日期的签到计数）
     *
     * @param date 日期
     * @return 递增后的计数值
     */
    long addRecord(LocalDate date);

    /**
     * 获取指定日期的签到计数（从Redis）
     *
     * @param date 日期
     * @return 签到计数，如果键不存在返回0
     */
    long getCheckinCount(LocalDate date);

    /**
     * 持久化Redis中的签到数据到数据库
     * 遍历日期，从Redis获取数据并写入MySQL
     *
     * @return 持久化成功的记录数
     */
    int persistToDatabase();
}