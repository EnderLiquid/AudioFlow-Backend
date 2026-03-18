package top.enderliquid.audioflow.common.task;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import top.enderliquid.audioflow.manager.CheckinCountManager;
import top.enderliquid.audioflow.manager.DauManager;

/**
 * 统计数据同步定时任务
 * 每5分钟将Redis中的日活和日签到数同步到MySQL数据库
 */
@Slf4j
@Component
public class StatsSyncTask {

    @Autowired
    private DauManager dauManager;

    @Autowired
    private CheckinCountManager checkinCountManager;

    /**
     * 每5分钟执行一次统计数据同步
     */
    @Scheduled(cron = "0 */5 * * * ?")
    public void syncStatsToDatabase() {
        log.info("开始同步统计数据到数据库");

        int dauSyncCount = dauManager.syncAllFromRedis();
        int checkinSyncCount = checkinCountManager.syncAllFromRedis();

        log.info("统计数据同步完成，日活同步: {}条，签到同步: {}条", dauSyncCount, checkinSyncCount);
    }
}