package top.enderliquid.audioflow.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.enderliquid.audioflow.common.annotation.RateLimit;
import top.enderliquid.audioflow.common.annotation.RateLimits;
import top.enderliquid.audioflow.common.enums.LimitType;
import top.enderliquid.audioflow.common.response.HttpResponseBody;
import top.enderliquid.audioflow.dto.request.loginlog.LoginLogPageDTO;
import top.enderliquid.audioflow.dto.response.PageVO;
import top.enderliquid.audioflow.dto.response.loginlog.LoginLogVO;
import top.enderliquid.audioflow.service.LoginLogService;

@RestController
@RequestMapping("/api/login-logs")
@Validated
@RequiredArgsConstructor
public class LoginLogController {

    private final LoginLogService loginLogService;

    /**
     * 查询登录流水
     */
    @SaCheckLogin
    @GetMapping
    @RateLimits({
            @RateLimit(type = LimitType.IP),
            @RateLimit(type = LimitType.USER)
    })
    public HttpResponseBody<PageVO<LoginLogVO>> page(@Valid LoginLogPageDTO dto) {
        long userId = StpUtil.getLoginIdAsLong();
        PageVO<LoginLogVO> result = loginLogService.page(userId, dto);
        return HttpResponseBody.ok(result, "查询登录流水成功");
    }
}