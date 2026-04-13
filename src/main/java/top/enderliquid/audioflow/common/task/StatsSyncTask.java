package top.enderliquid.audioflow.common.task;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import top.enderliquid.audioflow.dto.response.stats.StatsSyncVO;
import top.enderliquid.audioflow.service.StatsService;

/**
 * 统计数据同步定时任务
 * 每5分钟将Redis中的日活和日签到数同步到MySQL数据库，委托StatsService执行具体业务逻辑
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StatsSyncTask {

    private final StatsService statsService;

    /**
     * 每5分钟执行一次统计数据同步
     */
    @Scheduled(cron = "0 */5 * * * ?")
    public void syncStatsToDatabase() {
        StatsSyncVO result = statsService.syncStatsToDatabase();
        log.info("统计数据同步任务执行完毕，日活同步: {}条，签到同步: {}条",
                result.getDauSyncCount(), result.getCheckinSyncCount());
    }
}