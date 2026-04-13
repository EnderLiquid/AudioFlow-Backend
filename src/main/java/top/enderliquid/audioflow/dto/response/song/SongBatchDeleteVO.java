package top.enderliquid.audioflow.dto.response.song;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import top.enderliquid.audioflow.dto.response.BatchResult;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SongBatchDeleteVO {
    private BatchResult<Void> result;
}