package top.enderliquid.audioflow.dto.request.song;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

import static top.enderliquid.audioflow.common.constant.ValidationConstants.BATCH_SIZE_MAX;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SongBatchCompleteDTO {
    @NotEmpty(message = "歌曲ID列表不能为空")
    @Size(max = BATCH_SIZE_MAX, message = "批量确认数量不能超过{max}")
    private List<@NotNull(message = "歌曲Id不能为空") Long> songIds;
}