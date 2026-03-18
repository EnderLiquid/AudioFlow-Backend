package top.enderliquid.audioflow.manager;

import com.baomidou.mybatisplus.extension.service.IService;
import top.enderliquid.audioflow.entity.Dau;

import java.time.LocalDate;

/**
 * 日活统计管理器
 * 使用Redis HyperLogLog进行日活的粗略统计
 * 同时管理Redis缓存和MySQL持久化
 */
public interface DauManager extends IService<Dau> {

    /**
     * 添加日活记录
     *
     * @param userId 用户ID
     * @param date   日期
     */
    void addRecord(Long userId, LocalDate date);

    /**
     * 获取指定日期的日活数（从Redis）
     *
     * @param date 日期
     * @return 日活数，如果键不存在返回0
     */
    long getDauCount(LocalDate date);

    /**
     * 同步指定日期范围内所有日活数据到数据库
     * 遍历日期，从Redis获取数据并同步
     *
     * @return 同步成功的记录数
     */
    int syncAllFromRedis();
}