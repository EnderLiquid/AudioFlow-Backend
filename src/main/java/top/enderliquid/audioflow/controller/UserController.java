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
import top.enderliquid.audioflow.dto.request.user.UserSaveDTO;
import top.enderliquid.audioflow.dto.request.user.UserUpdatePasswordDTO;
import top.enderliquid.audioflow.dto.response.session.LoginResult;
import top.enderliquid.audioflow.dto.response.user.UserVO;
import top.enderliquid.audioflow.service.UserService;

@RestController
@RequestMapping("/api/users")
@Validated
public class UserController {

    @Autowired
    private UserService userService;

    /**
     * 用户注册
     */
    @PostMapping
    @RateLimits(
            value = @RateLimit(type = LimitType.IP, refillRate = "1/10", capacity = 5),
            message = "注册过于频繁，请稍后再试"
    )
    public HttpResponseBody<LoginResult> register(@Valid @RequestBody UserSaveDTO dto) {
        UserVO userVO = userService.saveUser(dto);
        StpUtil.login(userVO.getId());
        SaTokenInfo tokenInfo = StpUtil.getTokenInfo();
        return HttpResponseBody.ok(new LoginResult(userVO, tokenInfo), "注册成功");
    }

    /**
     * 获取当前登录用户信息
     */
    @SaCheckLogin
    @GetMapping("me")
    @RateLimits({
            @RateLimit(type = LimitType.IP),
            @RateLimit(type = LimitType.USER)
    })
    public HttpResponseBody<UserVO> getUserInfo() {
        long userId = StpUtil.getLoginIdAsLong();
        UserVO userVO = userService.getUser(userId);
        return HttpResponseBody.ok(userVO);
    }

    /**
     * 更改用户密码
     */
    @SaCheckLogin
    @PatchMapping("me/password")
    @RateLimits({
            @RateLimit(type = LimitType.IP),
            @RateLimit(type = LimitType.USER)
    })
    public HttpResponseBody<UserVO> changePassword(@Valid @RequestBody UserUpdatePasswordDTO dto) {
        long userId = StpUtil.getLoginIdAsLong();
        UserVO userVO = userService.updateUserPassword(dto, userId);
        StpUtil.kickout(userId);
        return HttpResponseBody.ok(userVO, "密码修改成功，请重新登录");
    }
}