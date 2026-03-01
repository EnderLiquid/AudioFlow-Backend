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
    private List<BatchResultItem<T>> successList = new ArrayList<>();
    private List<BatchResultItem<T>> failureList = new ArrayList<>();
    private int successCount;
    private int failureCount;
    private int total;

    // 辅助方法：添加结果并自动计数
    public void add(BatchResultItem<T> item) {
        this.total++;
        if (item.isSuccess()) {
            this.successCount++;
            this.successList.add(item);
        } else {
            this.failureCount++;
            this.failureList.add(item);
        }
    }

}