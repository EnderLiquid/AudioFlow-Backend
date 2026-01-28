package top.enderliquid.audioflow.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import top.enderliquid.audioflow.common.response.HttpResponseBody;
import top.enderliquid.audioflow.dto.request.UserSaveDTO;
import top.enderliquid.audioflow.dto.request.UserVerifyPasswordDTO;
import top.enderliquid.audioflow.dto.response.UserVO;
import top.enderliquid.audioflow.service.UserService;

@RestController
@RequestMapping("/api/user")
@Validated
public class UserController {

    @Autowired
    private UserService userService;

    /**
     * 用户注册
     */
    @PostMapping("/register")
    public HttpResponseBody<UserVO> register(@RequestBody @Valid UserSaveDTO dto) {
        UserVO userVO = userService.saveUser(dto);
        return HttpResponseBody.ok(userVO, "注册成功");
    }

    /**
     * 用户登录
     */
    @PostMapping("/login")
    public HttpResponseBody<UserVO> login(@RequestBody @Valid UserVerifyPasswordDTO dto) {
        UserVO userVO = userService.verifyUserPassword(dto);
        StpUtil.login(userVO.getId());
        return HttpResponseBody.ok(userVO, "登录成功");
    }

    /**
     * 用户注销
     */
    @SaCheckLogin
    @PostMapping("/logout")
    public HttpResponseBody<Void> logout() {
        StpUtil.logout();
        return HttpResponseBody.ok(null, "注销成功");
    }

    /**
     * 获取当前登录用户信息
     */
    @SaCheckLogin
    @GetMapping("info")
    public HttpResponseBody<UserVO> getUserInfo() {
        // 从 Sa-Token 获取当前登录的 ID
        long userId = StpUtil.getLoginIdAsLong();
        UserVO userVO = userService.getUser(userId);
        return HttpResponseBody.ok(userVO);
    }
}
