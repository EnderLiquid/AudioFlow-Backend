package top.enderliquid.audioflow.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BatchResultItem<T> {
    private Integer index;
    private Boolean success;
    private String message;
    private T data;

    public static <T> BatchResultItem<T> ok(Integer index, T data) {
        return new BatchResultItem<>(index, true, "OK", data);
    }

    public static <T> BatchResultItem<T> fail(Integer index, String message) {
        return new BatchResultItem<>(index, false, message, null);
    }
}
