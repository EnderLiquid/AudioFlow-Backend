package top.enderliquid.audioflow.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.SaTokenInfo;
import cn.dev33.satoken.stp.StpUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import top.enderliquid.audioflow.common.annotation.RateLimit;
import top.enderliquid.audioflow.common.annotation.RateLimits;
import top.enderliquid.audioflow.common.enums.LimitType;
import top.enderliquid.audioflow.common.response.HttpResponseBody;
import top.enderliquid.audioflow.common.util.RequestUtil;
import top.enderliquid.audioflow.dto.request.session.LoginContext;
import top.enderliquid.audioflow.dto.request.user.UserLoginDTO;
import top.enderliquid.audioflow.dto.response.session.LoginResult;
import top.enderliquid.audioflow.dto.response.user.UserVO;
import top.enderliquid.audioflow.service.SessionService;

@RestController
@RequestMapping("/api/sessions")
@Validated
@RequiredArgsConstructor
public class SessionController {

    private final SessionService sessionService;

    /**
     * 用户登录
     */
    @PostMapping
    @RateLimits(
            value = @RateLimit(type = LimitType.IP, refillRate = "1/10", capacity = 5),
            message = "登录尝试过于频繁，请稍后再试"
    )
    public HttpResponseBody<LoginResult> login(@Valid @RequestBody UserLoginDTO dto,
                                               HttpServletRequest request) {
        // 获取设备信息
        LoginContext context = new LoginContext();
        context.setIp(RequestUtil.getClientIp(request));
        context.setDeviceType(RequestUtil.getDeviceType(request));
        context.setOs(RequestUtil.getOs(request));
        context.setBrowser(RequestUtil.getBrowser(request));
        context.setUserAgent(RequestUtil.getUserAgent(request));

        UserVO userVO = sessionService.login(dto, context);
        StpUtil.login(userVO.getId());
        SaTokenInfo tokenInfo = StpUtil.getTokenInfo();
        return HttpResponseBody.ok(new LoginResult(userVO, tokenInfo), "登录成功");
    }

    /**
     * 用户注销（仅当前设备）
     */
    @SaCheckLogin
    @DeleteMapping("current")
    @RateLimits(@RateLimit(type = LimitType.IP))
    public HttpResponseBody<Void> logout() {
        StpUtil.logoutByTokenValue(StpUtil.getTokenValue());
        return HttpResponseBody.ok(null, "注销成功");
    }
}