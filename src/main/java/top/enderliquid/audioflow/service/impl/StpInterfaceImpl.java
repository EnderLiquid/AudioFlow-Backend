package top.enderliquid.audioflow.service.impl;

import cn.dev33.satoken.stp.StpInterface;

import java.util.ArrayList;
import java.util.List;

// 实现StpInterface接口，用于SaToken权限/角色获取
public class StpInterfaceImpl implements StpInterface {
    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        return new ArrayList<>();
    }

    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        return new ArrayList<>();
    }
}
