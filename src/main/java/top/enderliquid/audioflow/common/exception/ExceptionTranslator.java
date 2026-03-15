package top.enderliquid.audioflow.common.exception;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import cn.dev33.satoken.exception.NotRoleException;
import cn.dev33.satoken.exception.SaTokenException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
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
import top.enderliquid.audioflow.common.util.StrFormatter;

import java.util.stream.Collectors;

@Component
@Slf4j
public class ExceptionTranslator {

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
                yield new ExceptionTranslateResult(HttpStatus.FORBIDDEN, "鉴权失败");
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
                        StrFormatter.format("不支持的媒体类型: {}", e.getContentType()));
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
                yield new ExceptionTranslateResult(HttpStatus.BAD_REQUEST, StrFormatter.format("Body参数错误: {}", errorMessage));
            }

            /*
              处理 URL 参数校验异常 (@RequestParam / @PathVariable 上的 @Validated)
            */
            case ConstraintViolationException e -> {
                String errorMessage = e.getConstraintViolations().stream()
                        .map(ConstraintViolation::getMessage)
                        .collect(Collectors.joining("; "));
                log.warn("参数校验错误(URL): {}", errorMessage);
                yield new ExceptionTranslateResult(HttpStatus.BAD_REQUEST, StrFormatter.format("URL参数错误: {}", errorMessage));
            }

            /*
              处理表单绑定异常 (非 JSON 提交)
            */
            case BindException e -> {
                String errorMessage = e.getBindingResult().getFieldErrors().stream()
                        .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
                        .collect(Collectors.joining("; "));
                log.warn("参数绑定失败: {}", errorMessage);
                yield new ExceptionTranslateResult(HttpStatus.BAD_REQUEST, StrFormatter.format("参数绑定失败: {}", errorMessage));
            }

            /*
              缺少必要的 Query 参数 (@RequestParam(required = true))
            */
            case MissingServletRequestParameterException e -> {
                log.warn("请求缺少必要参数: {}", e.getParameterName());
                yield new ExceptionTranslateResult(HttpStatus.BAD_REQUEST, StrFormatter.format("请求缺少必要参数: {}", e.getParameterName()));
            }

            /*
              缺少必要的 Multipart 参数 (文件上传)
            */
            case MissingServletRequestPartException e -> {
                log.warn("请求缺少必要部分: {}", e.getRequestPartName());
                yield new ExceptionTranslateResult(HttpStatus.BAD_REQUEST, StrFormatter.format("请求缺少必要部分: {}", e.getRequestPartName()));
            }

            /*
              参数类型不匹配
              场景: 前端传了 String "abc" 给 Integer 类型的字段
            */
            case MethodArgumentTypeMismatchException e -> {
                String requiredType = e.getRequiredType() != null ? e.getRequiredType().getSimpleName() : "未知类型";
                log.warn("参数类型不匹配: {} 应为 {}", e.getName(), requiredType);
                yield new ExceptionTranslateResult(HttpStatus.BAD_REQUEST, StrFormatter.format("参数类型不匹配: {} 应为 {}", e.getName(), requiredType));
            }

            // ==================== 5. 路由与 HTTP 协议异常 ====================

            /*
              处理资源不存在异常 (Spring Boot 3.2+ 默认 404)
              场景: 静态资源或接口路径错误
            */
            case NoResourceFoundException e -> {
                log.warn("请求的资源不存在: [{} {}]", e.getHttpMethod(), e.getResourcePath());
                yield new ExceptionTranslateResult(HttpStatus.NOT_FOUND, StrFormatter.format("请求的资源不存在: [{} {}]", e.getHttpMethod(), e.getResourcePath()));
            }

            /*
              处理接口不存在异常 (旧版或手动配置 throw-exception-if-no-handler-found=true 时触发)
            */
            case NoHandlerFoundException e -> {
                log.warn("接口不存在: [{} {}]", e.getHttpMethod(), e.getRequestURL());
                yield new ExceptionTranslateResult(HttpStatus.NOT_FOUND, StrFormatter.format("接口不存在: [{} {}]", e.getHttpMethod(), e.getRequestURL()));
            }

            /*
              请求方法不支持
              场景: 接口定义了 POST，前端用了 GET
            */
            case HttpRequestMethodNotSupportedException e -> {
                log.warn("请求方法不支持: {}", e.getMethod());
                yield new ExceptionTranslateResult(HttpStatus.METHOD_NOT_ALLOWED, StrFormatter.format("请求方法不支持: {}", e.getMethod()));
            }

            // ==================== 5. 持久层异常 ====================

            /*
              键冲突 / 数据完整性违规 / 数据库宕机 / 连接池耗尽 / 网络异常 / SQL语法错误 / 查询超时 / 死锁
            */
            case DataAccessException e -> {
                log.error("数据库异常", e);
                yield new ExceptionTranslateResult(HttpStatus.INTERNAL_SERVER_ERROR, "数据库访问异常，请稍后再试");
            }

            // ==================== 6. 全局兜底异常 ====================

            /*
              处理所有未捕获的异常
            */
            case Exception e -> {
                log.error("未捕获异常", e);
                yield new ExceptionTranslateResult(HttpStatus.INTERNAL_SERVER_ERROR, StrFormatter.format("系统内部错误，请联系管理员"));
            }
        };
    }
}
