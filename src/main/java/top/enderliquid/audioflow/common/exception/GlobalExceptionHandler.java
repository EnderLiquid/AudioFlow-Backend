package top.enderliquid.audioflow.common.exception;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import cn.dev33.satoken.exception.NotRoleException;
import cn.dev33.satoken.exception.SaTokenException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import top.enderliquid.audioflow.common.response.HttpResponseBody;

import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ==================== 业务异常处理 ====================

    @ExceptionHandler(BusinessException.class)
    @ResponseStatus(HttpStatus.OK)
    public HttpResponseBody<?> handleBusinessException(BusinessException e) {
        log.warn("业务异常: {}", e.getMessage());
        return HttpResponseBody.fail(e.getMessage());
    }

    // ==================== Sa-Token 异常处理 ====================

    @ExceptionHandler(NotLoginException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public HttpResponseBody<?> handlerNotLoginException(NotLoginException e) {
        log.warn("未登录异常: {}", e.getMessage());
        return HttpResponseBody.fail(e.getMessage());
    }

    @ExceptionHandler(NotPermissionException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public HttpResponseBody<?> handlerNotLoginException(NotPermissionException e) {
        log.warn("无权限异常: {}", e.getMessage());
        return HttpResponseBody.fail(e.getMessage());
    }

    @ExceptionHandler(NotRoleException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public HttpResponseBody<?> handlerNotLoginException(NotRoleException e) {
        log.warn("无角色异常: {}", e.getMessage());
        return HttpResponseBody.fail(e.getMessage());
    }

    @ExceptionHandler(SaTokenException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public HttpResponseBody<?> handlerSaTokenException(SaTokenException e) {
        log.warn("鉴权异常: {}", e.getMessage());
        return HttpResponseBody.fail(e.getMessage());
    }

    // ==================== 参数校验异常处理 ====================

    /**
     * 处理 @Valid 参数校验异常（RequestBody参数）
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public HttpResponseBody<?> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        String errorMessage = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        log.warn("参数校验失败: {}", errorMessage);
        return HttpResponseBody.fail("参数错误: " + errorMessage);
    }

    /**
     * 处理 @Validated 参数校验异常（Query参数、PathVariable参数）
     */
    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public HttpResponseBody<?> handleConstraintViolationException(ConstraintViolationException e) {
        String errorMessage = e.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining("; "));
        log.warn("参数校验失败: {}", errorMessage);
        return HttpResponseBody.fail("参数错误: " + errorMessage);
    }

    /**
     * 处理参数绑定异常
     */
    @ExceptionHandler(BindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public HttpResponseBody<?> handleBindException(BindException e) {
        String errorMessage = e.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
                .collect(Collectors.joining("; "));
        log.warn("参数绑定失败: {}", errorMessage);
        return HttpResponseBody.fail("参数绑定失败: " + errorMessage);
    }

    /**
     * 处理缺少必要参数异常
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public HttpResponseBody<?> handleMissingServletRequestParameterException(MissingServletRequestParameterException e) {
        log.warn("缺少必要参数: {}", e.getParameterName());
        return HttpResponseBody.fail("缺少必要参数: " + e.getParameterName());
    }

    /**
     * 处理参数类型不匹配异常
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public HttpResponseBody<?> handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException e) {
        log.warn("参数类型不匹配: {} 应为 {}", e.getName(), e.getRequiredType() != null ? e.getRequiredType().getSimpleName() : "未知");
        return HttpResponseBody.fail("参数类型不匹配: " + e.getName());
    }

    // ==================== HTTP协议异常处理 ====================

    /**
     * 处理404异常（接口不存在）
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public HttpResponseBody<?> handleNoHandlerFoundException(NoHandlerFoundException e) {
        log.warn("接口不存在 [{} {}]", e.getHttpMethod(), e.getRequestURL());
        return HttpResponseBody.fail("接口不存在: " + e.getRequestURL());
    }

    /**
     * 处理请求方法不支持异常
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    public HttpResponseBody<?> handleHttpRequestMethodNotSupportedException(HttpRequestMethodNotSupportedException e) {
        log.warn("请求方法不支持: {}", e.getMethod());
        return HttpResponseBody.fail("请求方法不支持: " + e.getMethod());
    }

    // ==================== 未捕获异常处理 ====================

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public HttpResponseBody<?> handleException(Exception e) {
        log.error("系统异常: {}", e.getMessage());
        String message = "系统内部错误: " + e.getMessage();
        return HttpResponseBody.fail(message);
    }
}