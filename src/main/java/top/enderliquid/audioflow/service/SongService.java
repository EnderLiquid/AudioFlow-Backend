package top.enderliquid.audioflow.service;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.lang.Nullable;
import org.springframework.validation.annotation.Validated;
import top.enderliquid.audioflow.dto.request.song.SongBatchCompleteDTO;
import top.enderliquid.audioflow.dto.request.song.SongBatchDeleteDTO;
import top.enderliquid.audioflow.dto.request.song.SongBatchPrepareDTO;
import top.enderliquid.audioflow.dto.request.song.SongCompleteUploadDTO;
import top.enderliquid.audioflow.dto.request.song.SongPageDTO;
import top.enderliquid.audioflow.dto.request.song.SongPrepareUploadDTO;
import top.enderliquid.audioflow.dto.request.song.SongUpdateDTO;
import top.enderliquid.audioflow.dto.response.CommonPageVO;
import top.enderliquid.audioflow.dto.response.SongBatchResultVO;
import top.enderliquid.audioflow.dto.response.SongUploadPrepareVO;
import top.enderliquid.audioflow.dto.response.SongVO;

@Validated
public interface SongService {
    CommonPageVO<SongVO> pageSongsByUploaderKeywordAndSongKeyword(@Valid SongPageDTO dto);

    void removeSong(@NotNull(message = "歌曲Id不能为空") Long songId, @NotNull(message = "用户Id不能为空") Long userId);

    SongVO getSong(@NotNull(message = "歌曲Id不能为空") Long songId);

    @Nullable
    String getSongUrl(@NotNull(message = "歌曲Id不能为空") Long songId);

    SongVO updateSong(@Valid SongUpdateDTO dto, @NotNull(message = "歌曲Id不能为空") Long songId, @NotNull(message = "用户Id不能为空") Long userId);

    SongUploadPrepareVO prepareUpload(@Valid SongPrepareUploadDTO dto, @NotNull(message = "用户Id不能为空") Long userId);

    SongVO completeUpload(@Valid SongCompleteUploadDTO dto, @NotNull(message = "用户Id不能为空") Long userId);

    SongBatchResultVO<SongUploadPrepareVO> batchPrepareUpload(@Valid SongBatchPrepareDTO dto, @NotNull(message = "用户Id不能为空") Long userId);

    SongBatchResultVO<SongVO> batchCompleteUpload(@Valid SongBatchCompleteDTO dto, @NotNull(message = "用户Id不能为空") Long userId);

    SongBatchResultVO<Long> batchRemoveSongs(@Valid SongBatchDeleteDTO dto, @NotNull(message = "用户Id不能为空") Long userId);
}
