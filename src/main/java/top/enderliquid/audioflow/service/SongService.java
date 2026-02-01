package top.enderliquid.audioflow.service;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.lang.Nullable;
import org.springframework.validation.annotation.Validated;
import top.enderliquid.audioflow.dto.request.SongPageDTO;
import top.enderliquid.audioflow.dto.request.SongSaveDTO;
import top.enderliquid.audioflow.dto.request.SongUpdateDTO;
import top.enderliquid.audioflow.dto.response.CommonPageVO;
import top.enderliquid.audioflow.dto.response.SongVO;

@Validated
public interface SongService {
    SongVO saveSong(@Valid SongSaveDTO dto, @NotNull(message = "用户Id不能为空") Long userId);

    CommonPageVO<SongVO> pageSongsByUploaderKeywordAndSongKeyword(@Valid SongPageDTO dto);

    void removeSong(@NotNull(message = "歌曲Id不能为空") Long songId, @NotNull(message = "用户Id不能为空") Long userId);

    void removeSongForce(@NotNull(message = "用户Id不能为空") Long songId);

    SongVO getSong(@NotNull(message = "歌曲Id不能为空") Long songId);

    @Nullable
    String getSongUrl(@NotNull(message = "歌曲Id不能为空") Long songId);

    SongVO updateSong(@Valid SongUpdateDTO dto, @NotNull(message = "用户Id不能为空") Long userId);

    SongVO updateSongForce(@Valid SongUpdateDTO dto);
}
