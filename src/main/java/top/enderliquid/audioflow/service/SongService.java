package top.enderliquid.audioflow.service;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.multipart.MultipartFile;
import top.enderliquid.audioflow.dto.request.SongPageDTO;
import top.enderliquid.audioflow.dto.response.CommonPageVO;
import top.enderliquid.audioflow.dto.response.SongVO;

@Validated
public interface SongService {
    SongVO saveSong(@NotNull(message = "文件不能为空") MultipartFile file, @NotNull(message = "用户Id不能为空") Long userId);

    CommonPageVO<SongVO> pageSongsByUploaderKeywordAndSongKeyword(@Valid SongPageDTO dto);

    void removeSong(@NotNull(message = "歌曲Id不能为空") Long songId, @NotNull(message = "用户Id不能为空") Long userId);

    void removeSongForce(@NotNull(message = "用户Id不能为空") Long songId);
}
