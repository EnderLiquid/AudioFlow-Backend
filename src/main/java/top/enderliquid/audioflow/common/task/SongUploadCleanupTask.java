package top.enderliquid.audioflow.common.task;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import top.enderliquid.audioflow.common.enums.SongStatus;
import top.enderliquid.audioflow.common.transaction.TransactionHelper;
import top.enderliquid.audioflow.entity.Song;
import top.enderliquid.audioflow.manager.OSSManager;
import top.enderliquid.audioflow.manager.SongManager;
import top.enderliquid.audioflow.manager.UserManager;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static top.enderliquid.audioflow.common.enums.PointsType.SONG_UPLOAD_CANCEL;

@Slf4j
@Component
@RequiredArgsConstructor
public class SongUploadCleanupTask {

    @Value("${points.upload}")
    private int pointsPerUpload;

    @Value("${file.storage.s3.presigned-url-expiration}")
    private int presignedUrlExpirationSeconds;

    private final SongManager songManager;
    private final OSSManager ossManager;
    private final UserManager userManager;
    private final PlatformTransactionManager txManager;

    @Scheduled(cron = "0 0,20,40 * * * ? ")
    public void cleanupExpiredUploads() {
        log.info("开始清理过期歌曲记录");
        LocalDateTime cutoffTime = LocalDateTime.now().minusSeconds(presignedUrlExpirationSeconds);
        List<Song> expiredSongs = songManager.listByStatusesAndBeforeTime(
                Arrays.asList(SongStatus.UPLOADING, SongStatus.DELETING), cutoffTime);
        if (expiredSongs == null || expiredSongs.isEmpty()) {
            log.info("没有需要清理的过期歌曲记录");
            return;
        }
        int cleanedCount = 0;
        for (Song song : expiredSongs) {
            log.info("开始尝试清除过期歌曲记录，歌曲ID: {}, 状态: {}", song.getId(), song.getStatus());
            if (ossManager.checkFileExists(song.getFileName())) {
                if (!ossManager.deleteFile(song.getFileName())) {
                    log.error("从OSS删除已存在的歌曲文件失败，文件名: {}", song.getFileName());
                    continue;
                }
            } else {
                log.info("歌曲文件还未上传至OSS，跳过文件清除逻辑");
            }
            try (TransactionHelper tx = new TransactionHelper(txManager)) {
                if (song.getStatus() == SongStatus.UPLOADING) {
                    int balance = userManager.addPoints(song.getUploaderId(), pointsPerUpload, SONG_UPLOAD_CANCEL, song.getId());
                    if (balance < 0) {
                        log.info("返还用户积分失败");
                    }
                }
                if (!songManager.removeById(song)) {
                    log.info("删除歌曲记录失败");
                    continue;
                }
                tx.commit();
            }
            cleanedCount++;
        }
        log.info("清理过期歌曲记录完成，清理记录条数: {}", cleanedCount);
    }
}