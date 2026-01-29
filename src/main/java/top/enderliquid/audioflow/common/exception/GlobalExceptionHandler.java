package top.enderliquid.audioflow.common.exception;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import cn.dev33.satoken.exception.NotRoleException;
import cn.dev33.satoken.exception.SaTokenException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import top.enderliquid.audioflow.common.response.HttpResponseBody;

import java.util.stream.Collectors;

/**
 * 全局异常处理器
 * 用于统一处理 Controller 层抛出的各类异常，返回标准格式的 JSON 数据
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @Value("${response.exception.expose-uncaught-exception-detail:false}")
    boolean exposeUncaughtExceptionDetail;
    // ==================== 1. 业务逻辑异常 ====================

    /**
     * 处理自定义业务异常
     * 场景：业务代码中手动抛出的 BusinessException
     */
    @ExceptionHandler(BusinessException.class)
    @ResponseStatus(HttpStatus.OK) // 业务异常返回 200，通过 code 判断成功失败
    public HttpResponseBody<?> handleBusinessException(BusinessException e) {
        log.warn("业务异常: {}", e.getMessage());
        return HttpResponseBody.fail(e.getMessage());
    }

    // ==================== 2. Sa-Token 鉴权异常 ====================

    /**
     * 未登录异常
     */
    @ExceptionHandler(NotLoginException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED) // 401
    public HttpResponseBody<?> handlerNotLoginException(NotLoginException e) {
        log.warn("未登录异常: {}", e.getMessage());
        return HttpResponseBody.fail("用户未登录或Token无效");
    }

    /**
     * 无权限异常
     */
    @ExceptionHandler(NotPermissionException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN) // 403
    public HttpResponseBody<?> handlerNotPermissionException(NotPermissionException e) {
        log.warn("无权限异常: {}", e.getMessage());
        return HttpResponseBody.fail("无访问权限");
    }

    /**
     * 无角色异常
     */
    @ExceptionHandler(NotRoleException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN) // 403
    public HttpResponseBody<?> handlerNotRoleException(NotRoleException e) {
        log.warn("无角色异常: {}", e.getMessage());
        return HttpResponseBody.fail("无角色权限");
    }

    /**
     * Sa-Token 其他鉴权异常兜底
     */
    @ExceptionHandler(SaTokenException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN) // 403
    public HttpResponseBody<?> handlerSaTokenException(SaTokenException e) {
        log.warn("鉴权异常: {}", e.getMessage());
        return HttpResponseBody.fail("鉴权失败: " + e.getMessage());
    }

    // ==================== 3. 请求解析与格式异常 ====================

    /**
     * 请求体读取失败
     * 场景：前端未传 Body、Body 为空、JSON 格式错误
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public HttpResponseBody<?> handleHttpMessageNotReadableException(HttpMessageNotReadableException e) {
        log.warn("请求体读取失败: {}", e.getMessage());
        return HttpResponseBody.fail("请求体缺失或JSON格式错误，请检查发送的数据");
    }

    /**
     * 不支持的媒体类型
     * 场景：接口需要 application/json，前端传了 form-data
     */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    @ResponseStatus(HttpStatus.UNSUPPORTED_MEDIA_TYPE) // 415
    public HttpResponseBody<?> handleHttpMediaTypeNotSupportedException(HttpMediaTypeNotSupportedException e) {
        log.warn("不支持的媒体类型: {}", e.getContentType());
        return HttpResponseBody.fail("不支持的内容类型: " + e.getContentType());
    }

    /**
     * 文件上传大小超出限制
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public HttpResponseBody<?> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException e) {
        log.warn("文件上传大小超出限制: {}", e.getMessage());
        return HttpResponseBody.fail("文件大小超出限制，请检查配置");
    }

    // ==================== 4. 参数校验异常 ====================

    /**
     * 处理参数校验异常 (@Valid / @Validated)
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public HttpResponseBody<?> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        String errorMessage = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        log.warn("参数校验错误(Body): {}", errorMessage);
        return HttpResponseBody.fail("参数错误: " + errorMessage);
    }

    /**
     * 处理 URL 参数校验异常 (@RequestParam / @PathVariable 上的 @Validated)
     */
    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public HttpResponseBody<?> handleConstraintViolationException(ConstraintViolationException e) {
        String errorMessage = e.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining("; "));
        log.warn("参数校验错误(URL): {}", errorMessage);
        return HttpResponseBody.fail("参数错误: " + errorMessage);
    }

    /**
     * 处理表单绑定异常 (非 JSON 提交)
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
     * 缺少必要的 Query 参数 (@RequestParam(required = true))
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public HttpResponseBody<?> handleMissingServletRequestParameterException(MissingServletRequestParameterException e) {
        log.warn("缺少必要参数: {}", e.getParameterName());
        return HttpResponseBody.fail("请求缺少必要参数: " + e.getParameterName());
    }

    /**
     * 缺少必要的 Multipart 参数 (如文件上传)
     */
    @ExceptionHandler(MissingServletRequestPartException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public HttpResponseBody<?> handleMissingServletRequestPartException(MissingServletRequestPartException e) {
        log.warn("缺少必要部分: {}", e.getRequestPartName());
        return HttpResponseBody.fail("请求缺少必要部分: " + e.getRequestPartName());
    }

    /**
     * 参数类型不匹配
     * 场景：前端传了 String "abc" 给 Integer 类型的字段
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public HttpResponseBody<?> handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException e) {
        String requiredType = e.getRequiredType() != null ? e.getRequiredType().getSimpleName() : "未知类型";
        log.warn("参数类型不匹配: {} 应为 {}", e.getName(), requiredType);
        return HttpResponseBody.fail("参数类型不匹配: " + e.getName());
    }

    // ==================== 5. 路由与 HTTP 协议异常 ====================

    /**
     * 处理资源不存在异常 (Spring Boot 3.2+ 默认 404)
     * 场景：静态资源或接口路径错误
     */
    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public HttpResponseBody<?> handleNoResourceFoundException(NoResourceFoundException e) {
        log.warn("资源不存在: [{} {}]", e.getHttpMethod(), e.getResourcePath());
        return HttpResponseBody.fail("请求的资源不存在: " + e.getResourcePath());
    }

    /**
     * 处理接口不存在异常 (旧版或手动配置 throw-exception-if-no-handler-found=true 时触发)
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public HttpResponseBody<?> handleNoHandlerFoundException(NoHandlerFoundException e) {
        log.warn("接口不存在: [{} {}]", e.getHttpMethod(), e.getRequestURL());
        return HttpResponseBody.fail("接口不存在: " + e.getRequestURL());
    }

    /**
     * 请求方法不支持
     * 场景：接口定义了 POST，前端用了 GET
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED) // 405
    public HttpResponseBody<?> handleHttpRequestMethodNotSupportedException(HttpRequestMethodNotSupportedException e) {
        log.warn("请求方法不支持: {}", e.getMethod());
        return HttpResponseBody.fail("请求方法不支持: " + e.getMethod());
    }

    // ==================== 6. 全局兜底异常 ====================

    /**
     * 处理所有未捕获的异常
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR) // 500
    public HttpResponseBody<?> handleException(Exception e) {
        log.error("系统未知异常", e);
        if (exposeUncaughtExceptionDetail) return HttpResponseBody.fail("系统内部错误: " + e.getMessage());
        return HttpResponseBody.fail("系统内部错误，请联系管理员");
    }
}
