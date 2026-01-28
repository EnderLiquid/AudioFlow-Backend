package top.enderliquid.audioflow.common.config;

import cn.dev33.satoken.stp.StpInterface;
import org.springframework.beans.factory.annotation.Autowired;
import top.enderliquid.audioflow.common.exception.BusinessException;
import top.enderliquid.audioflow.dto.response.UserVO;
import top.enderliquid.audioflow.service.UserService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// 实现StpInterface接口，用于SaToken权限/角色获取
public class StpInterfaceImpl implements StpInterface {
    @Autowired
    private UserService userService;

    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        return new ArrayList<>();
    }

    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        long userId = Long.parseLong(loginId.toString());
        try {
            UserVO userVO = userService.getUser(userId);
            return Collections.singletonList(userVO.getRole().name());
        } catch (BusinessException e) {
            return new ArrayList<>();
        }
    }
}
