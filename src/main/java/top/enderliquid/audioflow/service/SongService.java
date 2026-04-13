package top.enderliquid.audioflow.service;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.lang.Nullable;
import org.springframework.validation.annotation.Validated;
import top.enderliquid.audioflow.dto.request.song.*;
import top.enderliquid.audioflow.dto.response.BatchResult;
import top.enderliquid.audioflow.dto.response.PageResult;
import top.enderliquid.audioflow.dto.response.song.SongPrepareUploadVO;
import top.enderliquid.audioflow.dto.response.song.SongVO;

@Validated
public interface SongService {
    PageResult<SongVO> pageSongsByUploaderKeywordAndSongKeyword(@Valid SongPageDTO dto);

    void removeSong(@NotNull(message = "歌曲Id不能为空") Long songId, @NotNull(message = "用户Id不能为空") Long userId);

    SongVO getSong(@NotNull(message = "歌曲Id不能为空") Long songId);

    @Nullable
    String getSongUrl(@NotNull(message = "歌曲Id不能为空") Long songId);

    SongVO updateSong(@Valid SongUpdateDTO dto, @NotNull(message = "歌曲Id不能为空") Long songId, @NotNull(message = "用户Id不能为空") Long userId);

    SongPrepareUploadVO prepareUpload(@Valid SongPrepareUploadDTO dto, @NotNull(message = "用户Id不能为空") Long userId);

    SongVO completeUpload(@Valid SongCompleteUploadDTO dto, @NotNull(message = "用户Id不能为空") Long userId);

    BatchResult<SongPrepareUploadVO> batchPrepareUpload(@Valid SongBatchPrepareDTO dto, @NotNull(message = "用户Id不能为空") Long userId);

    BatchResult<SongVO> batchCompleteUpload(@Valid SongBatchCompleteDTO dto, @NotNull(message = "用户Id不能为空") Long userId);

    BatchResult<Void> batchRemoveSongs(@Valid SongBatchDeleteDTO dto, @NotNull(message = "用户Id不能为空") Long userId);

    void cancelUpload(@NotNull(message = "歌曲Id不能为空") Long songId, @NotNull(message = "用户Id不能为空") Long userId);

    BatchResult<Void> batchCancelUpload(@Valid SongBatchCancelDTO dto, @NotNull(message = "用户Id不能为空") Long userId);

    /**
     * 清理过期的歌曲上传记录
     * 定时任务调用，清理状态为UPLOADING或DELETING且超过预签名URL有效期的记录
     *
     * @return 清理的记录条数
     */
    int cleanupExpiredUploads();
}
