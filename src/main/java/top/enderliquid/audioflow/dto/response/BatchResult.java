package top.enderliquid.audioflow.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BatchResult<T> {
    private List<BatchResultItem<T>> resultList = new ArrayList<>();
    private int successCount;
    private int failureCount;
}