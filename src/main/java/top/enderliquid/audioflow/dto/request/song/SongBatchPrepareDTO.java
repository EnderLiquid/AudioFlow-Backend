package top.enderliquid.audioflow.dto.request.song;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

import static top.enderliquid.audioflow.common.constant.ValidationConstants.BATCH_SIZE_MAX;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SongBatchPrepareDTO {
    @NotEmpty(message = "上传歌曲列表不能为空")
    @Size(max = BATCH_SIZE_MAX, message = "批量上传数量不能超过{max}")
    private List<@Valid SongPrepareUploadDTO> songs;
}