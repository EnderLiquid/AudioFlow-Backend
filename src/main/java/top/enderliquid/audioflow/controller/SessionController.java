package top.enderliquid.audioflow.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import top.enderliquid.audioflow.common.response.HttpResponseBody;
import top.enderliquid.audioflow.dto.request.user.UserVerifyPasswordDTO;
import top.enderliquid.audioflow.dto.response.UserVO;
import top.enderliquid.audioflow.service.UserService;

@RestController
@RequestMapping("/api/sessions")
@Validated
public class SessionController {

    @Autowired
    private UserService userService;

    /**
     * 用户登录
     */
    @PostMapping
    public HttpResponseBody<UserVO> login(@Valid @RequestBody UserVerifyPasswordDTO dto) {
        UserVO userVO = userService.verifyUserPassword(dto);
        StpUtil.login(userVO.getId());
        return HttpResponseBody.ok(userVO, "登录成功");
    }

    /**
     * 用户注销（仅当前设备）
     */
    @SaCheckLogin
    @DeleteMapping("current")
    public HttpResponseBody<Void> logout() {
        StpUtil.logoutByTokenValue(StpUtil.getTokenValue());
        return HttpResponseBody.ok(null, "注销成功");
    }
}
