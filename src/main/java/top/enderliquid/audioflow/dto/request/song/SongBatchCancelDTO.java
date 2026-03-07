package top.enderliquid.audioflow.dto.request.song;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SongBatchCancelDTO {
    @NotEmpty(message = "歌曲ID列表不能为空")
    @Size(max = 10, message = "批量取消数量不能超过10")
    private List<Long> songIds;
}