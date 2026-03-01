package top.enderliquid.audioflow.common.exception;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import cn.dev33.satoken.exception.NotRoleException;
import cn.dev33.satoken.exception.SaTokenException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.stereotype.Component;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.stream.Collectors;

@Component
@Slf4j
public class ExceptionTranslator {

    @Value("${response.exception.expose-uncaught-exception-detail:false}")
    boolean exposeUncaughtExceptionDetail;

    public ExceptionTranslateResult translate(Exception exception) {
        return switch (exception) {
            // ==================== 1. 业务逻辑异常 ====================

            /*
              处理自定义业务异常
              场景: 业务代码中手动抛出的 BusinessException
            */
            case BusinessException e -> {
                log.warn("业务异常: {}", e.getMessage());
                yield new ExceptionTranslateResult(HttpStatus.OK, e.getMessage());
            }

            /*
              接口限流异常
            */
            case RateLimitException e -> {
                log.warn("限流异常: {}", e.getMessage());
                yield new ExceptionTranslateResult(HttpStatus.FORBIDDEN, e.getMessage());
            }

            // ==================== 2. Sa-Token 鉴权异常 ====================

            /*
              未登录异常
            */
            case NotLoginException e -> {
                log.warn("未登录异常: {}", e.getMessage());
                yield new ExceptionTranslateResult(HttpStatus.UNAUTHORIZED, "用户未登录或Token无效");
            }

            /*
              无权限异常
            */
            case NotPermissionException e -> {
                log.warn("无权限异常: {}", e.getMessage());
                yield new ExceptionTranslateResult(HttpStatus.FORBIDDEN, "无访问权限");
            }


            /*
              无角色异常
            */
            case NotRoleException e -> {
                log.warn("无角色异常: {}", e.getMessage());
                yield new ExceptionTranslateResult(HttpStatus.FORBIDDEN, "无角色权限");
            }

            /*
              Sa-Token 其他鉴权异常兜底
            */
            case SaTokenException e -> {
                log.warn("鉴权异常: {}", e.getMessage());
                yield new ExceptionTranslateResult(HttpStatus.FORBIDDEN, "鉴权失败: {%s}".formatted(e.getMessage()));
            }

            // ==================== 3. 请求解析与格式异常 ====================

            /*
              请求体读取失败
              场景: 前端未传 Body、Body 为空、JSON 格式错误
            */
            case HttpMessageNotReadableException e -> {
                log.warn("请求体读取失败: {}", e.getMessage());
                yield new ExceptionTranslateResult(HttpStatus.BAD_REQUEST, "请求体缺失或JSON格式错误");
            }

            /*
              不支持的媒体类型
              场景: 接口需要 application/json，前端传了 form-data
            */
            case HttpMediaTypeNotSupportedException e -> {
                log.warn("不支持的媒体类型: {}", e.getContentType());
                yield new ExceptionTranslateResult(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                        "不支持的媒体类型: {%s}".formatted(e.getContentType()));
            }

            /*
              文件上传大小超出限制
            */
            case MaxUploadSizeExceededException e -> {
                log.warn("文件上传大小超出限制: {}", e.getMessage());
                yield new ExceptionTranslateResult(HttpStatus.BAD_REQUEST, "文件大小超出限制");
            }

            // ==================== 4. 参数校验异常 ====================

            /*
              处理参数校验异常 (@Valid / @Validated)
            */
            case MethodArgumentNotValidException e -> {
                String errorMessage = e.getBindingResult().getFieldErrors().stream()
                        .map(FieldError::getDefaultMessage)
                        .collect(Collectors.joining("; "));
                log.warn("参数校验错误(Body): {}", errorMessage);
                yield new ExceptionTranslateResult(HttpStatus.BAD_REQUEST, "Body参数错误: {%s}".formatted(errorMessage));
            }

            /*
              处理 URL 参数校验异常 (@RequestParam / @PathVariable 上的 @Validated)
            */
            case ConstraintViolationException e -> {
                String errorMessage = e.getConstraintViolations().stream()
                        .map(ConstraintViolation::getMessage)
                        .collect(Collectors.joining("; "));
                log.warn("参数校验错误(URL): {}", errorMessage);
                yield new ExceptionTranslateResult(HttpStatus.BAD_REQUEST, "URL参数错误: {%s}".formatted(errorMessage));
            }

            /*
              处理表单绑定异常 (非 JSON 提交)
            */
            case BindException e -> {
                String errorMessage = e.getBindingResult().getFieldErrors().stream()
                        .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
                        .collect(Collectors.joining("; "));
                log.warn("参数绑定失败: {}", errorMessage);
                yield new ExceptionTranslateResult(HttpStatus.BAD_REQUEST, "参数绑定失败: {%s}".formatted(errorMessage));
            }

            /*
              缺少必要的 Query 参数 (@RequestParam(required = true))
            */
            case MissingServletRequestParameterException e -> {
                log.warn("请求缺少必要参数: {}", e.getParameterName());
                yield new ExceptionTranslateResult(HttpStatus.BAD_REQUEST, "请求缺少必要参数: {%s}".formatted(e.getParameterName()));
            }

            /*
              缺少必要的 Multipart 参数 (文件上传)
            */
            case MissingServletRequestPartException e -> {
                log.warn("请求缺少必要部分: {}", e.getRequestPartName());
                yield new ExceptionTranslateResult(HttpStatus.BAD_REQUEST, "请求缺少必要部分: {%s}".formatted(e.getRequestPartName()));
            }

            /*
              参数类型不匹配
              场景: 前端传了 String "abc" 给 Integer 类型的字段
            */
            case MethodArgumentTypeMismatchException e -> {
                String requiredType = e.getRequiredType() != null ? e.getRequiredType().getSimpleName() : "未知类型";
                log.warn("参数类型不匹配: {} 应为 {}", e.getName(), requiredType);
                yield new ExceptionTranslateResult(HttpStatus.BAD_REQUEST, "参数类型不匹配: {%s} 应为 {%s}".formatted(e.getName(), requiredType));
            }

            // ==================== 5. 路由与 HTTP 协议异常 ====================

            /*
              处理资源不存在异常 (Spring Boot 3.2+ 默认 404)
              场景: 静态资源或接口路径错误
            */
            case NoResourceFoundException e -> {
                log.warn("请求的资源不存在: [{} {}]", e.getHttpMethod(), e.getResourcePath());
                yield new ExceptionTranslateResult(HttpStatus.NOT_FOUND, "请求的资源不存在: [{%s} {%s}]".formatted(e.getHttpMethod(), e.getResourcePath()));
            }

            /*
              处理接口不存在异常 (旧版或手动配置 throw-exception-if-no-handler-found=true 时触发)
            */
            case NoHandlerFoundException e -> {
                log.warn("接口不存在: [{} {}]", e.getHttpMethod(), e.getRequestURL());
                yield new ExceptionTranslateResult(HttpStatus.NOT_FOUND, "接口不存在: [{%s} {%s}]".formatted(e.getHttpMethod(), e.getRequestURL()));
            }

            /*
              请求方法不支持
              场景: 接口定义了 POST，前端用了 GET
            */
            case HttpRequestMethodNotSupportedException e -> {
                log.warn("请求方法不支持: {}", e.getMethod());
                yield new ExceptionTranslateResult(HttpStatus.METHOD_NOT_ALLOWED, "请求方法不支持: {%s}".formatted(e.getMethod()));
            }

            // ==================== 6. 全局兜底异常 ====================

            /*
              处理所有未捕获的异常
            */
            case Exception e -> {
                log.error("未捕获异常", e);
                String requestID = MDC.get("requestId");
                if (requestID == null) requestID = "未分配";
                if (exposeUncaughtExceptionDetail) {
                    yield new ExceptionTranslateResult(HttpStatus.INTERNAL_SERVER_ERROR, "系统内部错误: {%s}，请求ID: {%s}".formatted(e.getMessage(), requestID));
                }
                yield new ExceptionTranslateResult(HttpStatus.INTERNAL_SERVER_ERROR, "系统内部错误，请联系管理员，请求ID: {%s}".formatted(requestID));
            }
        };
    }
}
