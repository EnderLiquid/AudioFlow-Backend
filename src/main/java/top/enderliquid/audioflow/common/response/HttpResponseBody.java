package top.enderliquid.audioflow.common.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class HttpResponseBody<T> {
    private Boolean success;
    private String message;
    private T data;

    // 成功快捷方法
    public static <T> HttpResponseBody<T> ok(T data) {
        return new HttpResponseBody<>(true, "", data);
    }

    public static <T> HttpResponseBody<T> ok(T data, String message) {
        return new HttpResponseBody<>(true, message, data);
    }

    // 失败快捷方法
    public static <T> HttpResponseBody<T> fail(String message) {
        return new HttpResponseBody<>(false, message, null);
    }
}