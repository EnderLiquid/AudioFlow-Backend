package top.enderliquid.audioflow.dto.request.song;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

import static top.enderliquid.audioflow.common.constant.ValidationConstants.BATCH_SIZE_MAX;
import static top.enderliquid.audioflow.common.constant.ValidationConstants.BATCH_SIZE_MIN;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SongBatchPrepareDTO {
    @Valid
    @Size(min = BATCH_SIZE_MIN, max = BATCH_SIZE_MAX, message = "批量上传数量必须在" + BATCH_SIZE_MIN + "-" + BATCH_SIZE_MAX + "之间")
    private List<SongPrepareUploadDTO> songs;
}