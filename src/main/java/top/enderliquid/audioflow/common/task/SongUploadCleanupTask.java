package top.enderliquid.audioflow.common.task;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;
import top.enderliquid.audioflow.common.enums.SongStatus;
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
    private TransactionTemplate transactionTemplate;

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
            // 在事务内恢复积分并删除歌曲记录
            boolean success = Boolean.TRUE.equals(transactionTemplate.execute((status) -> {
                // 查询上传者用户
                User user = userManager.getById(song.getUploaderId());
                if (user != null) {
                    // 恢复积分
                    user.setPoints(user.getPoints() + pointsPerUpload);
                    if (!userManager.updateById(user)) {
                        log.error("恢复用户积分失败，用户ID: {}", user.getId());
                        return false;
                    }
                    log.info("恢复用户积分成功，用户ID: {}，当前积分: {}", user.getId(), user.getPoints());
                }
                // 删除歌曲记录
                if (!songManager.removeById(song)) {
                    log.error("删除歌曲记录失败，歌曲ID: {}", song.getId());
                    return false;
                }
                return true;
            }));
            if (success) {
                // 删除OSS文件（在事务外执行）
                if (ossManager.checkFileExists(song.getFileName())) {
                    if (ossManager.deleteFile(song.getFileName())) {
                        log.info("删除OSS文件成功，文件名: {}", song.getFileName());
                    } else {
                        log.error("删除OSS文件失败，文件名: {}", song.getFileName());
                    }
                }
                cleanedCount++;
            } else {
                log.error("清理超时上传记录失败，歌曲ID: {}", song.getId());
            }
        }
        log.info("清理超时上传记录完成，清理记录条数: {}", cleanedCount);
    }
}