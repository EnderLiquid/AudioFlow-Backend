package top.enderliquid.audioflow.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import top.enderliquid.audioflow.dto.response.stats.StatsSyncVO;
import top.enderliquid.audioflow.manager.CheckinCountManager;
import top.enderliquid.audioflow.manager.DauManager;
import top.enderliquid.audioflow.service.StatsService;

/**
 * 统计数据服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StatsServiceImpl implements StatsService {

    private final DauManager dauManager;
    private final CheckinCountManager checkinCountManager;

    @Override
    public StatsSyncVO syncStatsToDatabase() {
        log.info("开始同步统计数据到数据库");

        int dauSyncCount = dauManager.persistToDatabase();
        int checkinSyncCount = checkinCountManager.persistToDatabase();

        log.info("统计数据同步完成，日活同步: {}条，签到同步: {}条", dauSyncCount, checkinSyncCount);
        return new StatsSyncVO(dauSyncCount, checkinSyncCount);
    }
}