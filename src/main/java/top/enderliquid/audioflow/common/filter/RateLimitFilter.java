package top.enderliquid.audioflow.common.filter;

import cn.dev33.satoken.stp.StpUtil;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import top.enderliquid.audioflow.common.annotation.RateLimit;
import top.enderliquid.audioflow.common.enums.LimitType;
import top.enderliquid.audioflow.common.exception.RateLimitException;
import top.enderliquid.audioflow.common.service.RateLimitService;

import java.io.IOException;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import com.fasterxml.jackson.databind.ObjectMapper;

@Slf4j
@Component
@WebFilter(urlPatterns = "/*")
public class RateLimitFilter implements Filter {

    @Autowired
    private RateLimitService rateLimitService;

    @Autowired
    @Qualifier("requestMappingHandlerMapping")
    private RequestMappingHandlerMapping handlerMapping;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;

        try {
            RateLimit rateLimit = findRateLimitAnnotation(httpRequest);
            if (rateLimit == null) {
                chain.doFilter(request, response);
                return;
            }

            applyRateLimit(httpRequest, rateLimit);
            chain.doFilter(request, response);
        } catch (RateLimitException e) {
            handleRateLimitException((jakarta.servlet.http.HttpServletResponse) response, e);
        }
    }

    private RateLimit findRateLimitAnnotation(HttpServletRequest request) {
        try {
            HandlerExecutionChain handlerChain = handlerMapping.getHandler(request);
            if (handlerChain == null) {
                return null;
            }

            Object handler = handlerChain.getHandler();
            if (handler instanceof HandlerMethod) {
                HandlerMethod handlerMethod = (HandlerMethod) handler;
                return AnnotationUtils.findAnnotation(handlerMethod.getMethod(), RateLimit.class);
            }
        } catch (Exception e) {
            log.warn("查找@RateLimit注解失败", e);
        }
        return null;
    }

    private void applyRateLimit(HttpServletRequest request, RateLimit rateLimit) {
        String ip = getClientIp(request);
        Long userId = getUserId();
        String apiPath = request.getRequestURI();

        String refillRate = rateLimit.refillRate();
        String capacity = rateLimit.capacity();
        LimitType limitType = rateLimit.limitType();

        int capacityInt = Integer.parseInt(capacity);
        double refillRateDouble = rateLimitService.parseRefillRate(refillRate);

        boolean ipLimitPassed = true;
        boolean userLimitPassed = true;

        if (limitType == LimitType.BOTH || limitType == LimitType.IP) {
            String ipKey = rateLimitService.generateKey(ip, userId, apiPath, LimitType.IP);
            ipLimitPassed = rateLimitService.tryAcquire(ipKey, capacityInt, refillRateDouble, 1);
        }

        if (limitType == LimitType.BOTH || limitType == LimitType.USER) {
            String userKey = rateLimitService.generateKey(ip, userId, apiPath, LimitType.USER);
            userLimitPassed = rateLimitService.tryAcquire(userKey, capacityInt, refillRateDouble, 1);
        }

        if (limitType == LimitType.BOTH) {
            if (!ipLimitPassed || !userLimitPassed) {
                throw new RateLimitException(rateLimit.message());
            }
        } else if (limitType == LimitType.IP && !ipLimitPassed) {
            throw new RateLimitException(rateLimit.message());
        } else if (limitType == LimitType.USER && !userLimitPassed) {
            throw new RateLimitException(rateLimit.message());
        }
    }

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

    private Long getUserId() {
        try {
            return StpUtil.getLoginIdAsLong();
        } catch (Exception e) {
            return null;
        }
    }

    private void handleRateLimitException(jakarta.servlet.http.HttpServletResponse response, RateLimitException e) throws IOException {
        response.setStatus(403);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(
            "{\"success\":false,\"message\":\"" + e.getMessage() + "\",\"data\":null,\"code\":403}"
        );
    }
}
