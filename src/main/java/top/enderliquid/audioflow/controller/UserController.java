package top.enderliquid.audioflow.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import top.enderliquid.audioflow.common.response.HttpResponseBody;
import top.enderliquid.audioflow.dto.request.UserSaveDTO;
import top.enderliquid.audioflow.dto.request.UserUpdatePasswordDTO;
import top.enderliquid.audioflow.dto.response.UserVO;

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
    public HttpResponseBody<UserVO> register(@RequestBody UserSaveDTO dto) {
        UserVO userVO = userService.saveUser(dto);
        StpUtil.login(userVO.getId());
        return HttpResponseBody.ok(userVO, "注册成功");
    }

    /**
     * 获取当前登录用户信息
     */
    @SaCheckLogin
    @GetMapping("me")
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
    public HttpResponseBody<UserVO> changePassword(@RequestBody UserUpdatePasswordDTO dto) {
        long userId = StpUtil.getLoginIdAsLong();
        UserVO userVO = userService.updateUserPassword(dto, userId);
        StpUtil.kickout(userId);
        return HttpResponseBody.ok(userVO, "密码修改成功，请重新登录");
    }
}

