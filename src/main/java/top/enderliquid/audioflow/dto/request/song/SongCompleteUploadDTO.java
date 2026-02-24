package top.enderliquid.audioflow.dto.request.song;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SongCompleteUploadDTO {
    @NotNull(message = "歌曲ID不能为空")
    private Long songId;
}