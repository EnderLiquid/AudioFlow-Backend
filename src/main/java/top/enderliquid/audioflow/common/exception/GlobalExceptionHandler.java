package top.enderliquid.audioflow.common.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import top.enderliquid.audioflow.common.response.HttpResponseBody;

/**
 * 全局异常处理器
 * 用于统一处理 Controller 层抛出的各类异常，返回标准格式的 JSON 数据
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @Autowired
    private ExceptionTranslator translator;

    @ExceptionHandler(Exception.class) // 拦截所有
    public ResponseEntity<HttpResponseBody<?>> handleAll(Exception e) {
        // 1. 翻译
        ExceptionTranslateResult result = translator.translate(e);

        // 2. 构造返回体
        HttpResponseBody<?> body = HttpResponseBody.fail(result.getMessage());

        // 3. 返回 ResponseEntity，动态控制 HttpStatus
        return new ResponseEntity<>(body, result.getHttpStatus());
    }
}
