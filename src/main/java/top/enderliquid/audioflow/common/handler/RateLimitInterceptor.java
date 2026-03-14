package top.enderliquid.audioflow.common.handler;

import cn.dev33.satoken.stp.StpUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import top.enderliquid.audioflow.common.annotation.RateLimit;
import top.enderliquid.audioflow.common.annotation.RateLimits;
import top.enderliquid.audioflow.service.RateLimitService;

/**
 * 限流拦截器
 * 用于拦截带有 @RateLimits 注解的请求并进行限流检查
 */
@Slf4j
@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    @Autowired
    private RateLimitService rateLimitService;

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request,@NonNull HttpServletResponse response,@NonNull Object handler) {
        // 只处理方法请求，不处理静态资源等
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        RateLimits rateLimits = handlerMethod.getMethodAnnotation(RateLimits.class);

        // 如果方法没有 @RateLimits 注解，直接放行
        if (rateLimits == null) {
            return true;
        }

        String ip = getClientIp(request);
        Long userId = getUserId();
        String methodName = handlerMethod.getMethod().getName();
        String entry = rateLimits.entry().isEmpty() ? methodName : rateLimits.entry();

        // 执行所有限流规则检查
        for (RateLimit limit : rateLimits.value()) {
            rateLimitService.verifyRateLimit(limit, ip, userId, entry, rateLimits.message());
        }

        return true;
    }

    /**
     * 获取客户端 IP 地址
     * 优先从 X-Real-IP 和 X-Forwarded-For 头中获取，否则使用 remoteAddr
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Real-IP");
        if (ip == null || ip.isEmpty()) {
            ip = request.getHeader("X-Forwarded-For");
            if (ip != null && !ip.isEmpty()) {
                ip = ip.split(",")[0].trim();
            }
        }
        if (ip == null || ip.isEmpty()) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }

    /**
     * 获取当前登录用户ID
     * 如果用户未登录，返回 null
     */
    private Long getUserId() {
        try {
            // 经过测试，Sa-Token 可能在 Filter 层就已经完成了 LoginId 的解析，因此能够正常读取ID！
            return StpUtil.getLoginIdAsLong();
        } catch (Exception e) {
            return null;
        }
    }
}
