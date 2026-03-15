package top.enderliquid.audioflow.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.SaTokenInfo;
import cn.dev33.satoken.stp.StpUtil;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import top.enderliquid.audioflow.common.annotation.RateLimit;
import top.enderliquid.audioflow.common.annotation.RateLimits;
import top.enderliquid.audioflow.common.enums.LimitType;
import top.enderliquid.audioflow.common.response.HttpResponseBody;
import top.enderliquid.audioflow.dto.request.user.UserLoginDTO;
import top.enderliquid.audioflow.dto.response.session.LoginResult;
import top.enderliquid.audioflow.dto.response.user.UserVO;
import top.enderliquid.audioflow.service.SessionService;

@RestController
@RequestMapping("/api/sessions")
@Validated
public class SessionController {

    @Autowired
    private SessionService sessionService;

    /**
     * 用户登录
     */
    @PostMapping
    @RateLimits(
            value = @RateLimit(type = LimitType.IP, refillRate = "1/60", capacity = 3),
            value = @RateLimit(type = LimitType.IP, refillRate = "1/10", capacity = 5),
            message = "登录尝试过于频繁，请稍后再试"
    )
    public HttpResponseBody<LoginResult> login(@Valid @RequestBody UserLoginDTO dto) {
        UserVO userVO = sessionService.login(dto);
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