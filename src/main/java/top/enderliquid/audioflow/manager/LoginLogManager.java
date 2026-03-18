package top.enderliquid.audioflow.manager;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import top.enderliquid.audioflow.common.enums.LoginFailReason;
import top.enderliquid.audioflow.dto.request.loginlog.LoginLogPageDTO;
import top.enderliquid.audioflow.dto.request.session.LoginContext;
import top.enderliquid.audioflow.entity.LoginLog;

/**
 * 登录流水 Manager
 */
public interface LoginLogManager extends IService<LoginLog> {
    Page<LoginLog> pageByUserId(Long userId, LoginLogPageDTO dto);

    void addRecord(Long userId, String email, boolean success, LoginFailReason failReason, LoginContext context);
}