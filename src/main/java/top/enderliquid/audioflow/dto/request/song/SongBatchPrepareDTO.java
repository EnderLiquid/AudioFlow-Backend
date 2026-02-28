package top.enderliquid.audioflow.dto.request.song;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SongBatchPrepareDTO {
    @Valid
    @Size(min = 1, max = 10, message = "批量上传数量必须在1-10之间")
    private List<SongPrepareUploadDTO> songs;
}