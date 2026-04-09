package top.enderliquid.audioflow.common.task;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import top.enderliquid.audioflow.service.SongService;

/**
 * 歌曲上传清理定时任务
 * 定时清理过期的歌曲上传记录，委托SongService执行具体业务逻辑
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SongUploadCleanupTask {

    private final SongService songService;

    /**
     * 每小时的0分、20分、40分执行清理
     * 清理状态为UPLOADING或DELETING且超过预签名URL有效期的记录
     */
    @Scheduled(cron = "0 0,20,40 * * * ? ")
    public void cleanupExpiredUploads() {
        int cleanedCount = songService.cleanupExpiredUploads();
        log.info("歌曲上传清理任务执行完毕，清理记录条数: {}", cleanedCount);
    }
}