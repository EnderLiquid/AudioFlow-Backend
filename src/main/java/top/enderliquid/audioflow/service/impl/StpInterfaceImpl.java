package top.enderliquid.audioflow.service.impl;

import cn.dev33.satoken.stp.StpInterface;
import org.springframework.beans.factory.annotation.Autowired;
import top.enderliquid.audioflow.entity.User;
import top.enderliquid.audioflow.manager.UserManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// 实现StpInterface接口，用于SaToken权限/角色获取
public class StpInterfaceImpl implements StpInterface {
    @Autowired
    private UserManager userManager;

    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        return new ArrayList<>();
    }

    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        long userId = Long.parseLong(loginId.toString());
        User user = userManager.getById(userId);
        if (user == null) return new ArrayList<>();
        return Collections.singletonList(user.getRole().name());
    }
}
