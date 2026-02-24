package top.enderliquid.audioflow.task;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import top.enderliquid.audioflow.entity.Song;
import top.enderliquid.audioflow.manager.OSSManager;
import top.enderliquid.audioflow.manager.SongManager;
import top.enderliquid.audioflow.common.enums.SongStatus;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
public class SongUploadCleanupTask {

    @Autowired
    private SongManager songManager;

    @Autowired
    private OSSManager ossManager;

    @Scheduled(cron = "0 0 * * * ?")
    public void cleanupExpiredUploads() {
        log.info("开始清理超时上传记录");
        LocalDateTime cutoffTime = LocalDateTime.now().minusHours(1);
        List<Song> expiredSongs = songManager.listByStatusAndBeforeTime(
                SongStatus.UPLOADING.name(), cutoffTime);
        if (expiredSongs == null || expiredSongs.isEmpty()) {
            log.info("没有需要清理的超时上传记录");
            return;
        }
        int cleanedCount = 0;
        for (Song song : expiredSongs) {
            if (ossManager.checkFileExists(song.getFileName())) {
                ossManager.deleteFile(song.getFileName());
            }
            songManager.removeById(song);
            cleanedCount++;
        }
        log.info("清理超时上传记录完成，共清理{}条记录", cleanedCount);
    }
}