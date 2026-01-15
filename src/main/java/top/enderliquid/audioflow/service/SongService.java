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
    SongVO saveSong(@NotNull MultipartFile file, @NotNull Long userId);

    CommonPageVO<SongVO> pageSongsByUploaderKeywordAndSongKeyword(@Valid SongPageDTO dto);

    void removeSong(@NotNull Long songId, @NotNull Long userId);

    void removeSongForce(@NotNull Long songId);
}
