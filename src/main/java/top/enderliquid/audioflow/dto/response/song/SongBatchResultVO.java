package top.enderliquid.audioflow.dto.response.song;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import top.enderliquid.audioflow.dto.response.BatchFailureItem;

import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SongBatchResultVO<T> {
    private List<T> successList = new ArrayList<>();
    private List<BatchFailureItem> failureList = new ArrayList<>();
    private int successCount;
    private int failureCount;
}