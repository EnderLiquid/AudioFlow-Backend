package top.enderliquid.audioflow.service;

import org.springframework.validation.annotation.Validated;
import top.enderliquid.audioflow.dto.response.stats.StatsSyncVO;

/**
 * 统计数据服务
 * 处理日活、签到等统计数据的同步和查询
 */
@Validated
public interface StatsService {

    /**
     * 同步Redis中的统计数据到数据库
     * 包括日活数据和日签到数据
     *
     * @return 同步结果，包含各类型数据的同步条数
     */
    StatsSyncVO syncStatsToDatabase();
}