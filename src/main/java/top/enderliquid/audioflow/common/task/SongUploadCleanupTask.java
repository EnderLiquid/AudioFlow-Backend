package top.enderliquid.audioflow.common.task;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import top.enderliquid.audioflow.common.enums.SongStatus;
import top.enderliquid.audioflow.common.transaction.TransactionHelper;
import top.enderliquid.audioflow.entity.Song;
import top.enderliquid.audioflow.entity.User;
import top.enderliquid.audioflow.manager.OSSManager;
import top.enderliquid.audioflow.manager.SongManager;
import top.enderliquid.audioflow.manager.UserManager;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
public class SongUploadCleanupTask {

    @Value("${file.upload.points-per-upload:10}")
    private int pointsPerUpload = 10;

    @Autowired
    private SongManager songManager;

    @Autowired
    private OSSManager ossManager;

    @Autowired
    private UserManager userManager;

    @Autowired
    private PlatformTransactionManager txManager;

    @Scheduled(cron = "0 0,20,40 * * * ? ")
    public void cleanupExpiredUploads() {
        log.info("开始清理上传超时的歌曲记录");
        LocalDateTime cutoffTime = LocalDateTime.now().minusMinutes(30);
        List<Song> expiredSongs = songManager.listByStatusAndBeforeTime(
                SongStatus.UPLOADING, cutoffTime);
        if (expiredSongs == null || expiredSongs.isEmpty()) {
            log.info("没有需要清理的上传超时的歌曲记录");
            return;
        }
        int cleanedCount = 0;
        for (Song song : expiredSongs) {
            log.info("开始尝试清除上传超时的歌曲记录，歌曲ID: {}", song.getId());
            if (ossManager.checkFileExists(song.getFileName())) {
                if (!ossManager.deleteFile(song.getFileName())) {
                    log.error("从OSS删除已存在的歌曲文件失败，文件名: {}", song.getFileName());
                    continue;
                }
            } else {
                log.info("歌曲文件还未上传至OSS，跳过文件清除逻辑");
            }
            try (TransactionHelper tx = new TransactionHelper(txManager)) {
                User uploader = userManager.getById(song.getUploaderId());
                if (uploader != null) {
                    // 恢复积分
                    uploader.setPoints(uploader.getPoints() + pointsPerUpload);
                    if (!userManager.updateById(uploader)) {
                        log.info("更新用户积分失败，用户ID: {}", uploader.getId());
                        continue;
                    }
                    if (!songManager.removeById(song)) {
                        log.info("删除歌曲记录失败");
                        continue;
                    }
                } else {
                    log.info("用户已不存在，跳过积分恢复逻辑");
                }
                tx.commit();
            }
            cleanedCount++;
        }
        log.info("清理上传超时的歌曲记录完成，清理记录条数: {}", cleanedCount);
    }
}