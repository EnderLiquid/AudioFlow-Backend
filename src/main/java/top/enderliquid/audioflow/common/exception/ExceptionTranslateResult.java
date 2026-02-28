package top.enderliquid.audioflow.common.exception;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ExceptionTranslateResult {
    HttpStatus httpStatus;
    String message;
}
