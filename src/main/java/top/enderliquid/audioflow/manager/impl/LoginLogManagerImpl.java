package top.enderliquid.audioflow.manager.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Repository;
import top.enderliquid.audioflow.dto.request.session.LoginContext;
import top.enderliquid.audioflow.entity.LoginLog;
import top.enderliquid.audioflow.manager.LoginLogManager;
import top.enderliquid.audioflow.mapper.LoginLogMapper;

import static top.enderliquid.audioflow.common.constant.FieldLengthConstants.*;

/**
 * 登录流水 Manager 实现
 */
@Repository
public class LoginLogManagerImpl extends ServiceImpl<LoginLogMapper, LoginLog> implements LoginLogManager {

    /**
     * 分页查询用户登录流水
     * @param userId 用户ID
     * @param pageIndex 页码
     * @param pageSize 每页大小
     * @param asc 是否升序
     * @return 登录流水分页结果
     */
    @Override
    public Page<LoginLog> pageByUserId(Long userId, Long pageIndex, Long pageSize, boolean asc) {
        Page<LoginLog> page = new Page<>(pageIndex, pageSize);
        LambdaQueryWrapper<LoginLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(LoginLog::getUserId, userId)
               .eq(LoginLog::isSuccess, true);
        if (asc) {
            wrapper.orderByAsc(LoginLog::getCreateTime);
        } else {
            wrapper.orderByDesc(LoginLog::getCreateTime);
        }
        return super.page(page, wrapper);
    }

    public void addRecord(Long userId, String email, boolean success, String failReason, LoginContext context) {
        LoginLog loginLog = new LoginLog();
        loginLog.setUserId(userId);
        loginLog.setEmail(email);
        loginLog.setSuccess(success);
        loginLog.setFailReason(truncate(failReason, LOGIN_LOG_FAIL_REASON_MAX));
        loginLog.setIp(truncate(context.getIp(), LOGIN_LOG_IP_MAX));
        loginLog.setDeviceType(truncate(context.getDeviceType(), LOGIN_LOG_DEVICE_TYPE_MAX));
        loginLog.setOs(truncate(context.getOs(), LOGIN_LOG_OS_MAX));
        loginLog.setBrowser(truncate(context.getBrowser(), LOGIN_LOG_BROWSER_MAX));
        loginLog.setUserAgent(truncate(context.getUserAgent(), LOGIN_LOG_USER_AGENT_MAX));
        super.save(loginLog);
    }

    /**
     * 截断字符串到指定长度
     * @param str 待截断的字符串
     * @param maxLength 最大长度
     * @return 截断后的字符串，null返回null
     */
    private String truncate(String str, int maxLength) {
        if (str == null) {
            return null;
        }
        return str.length() <= maxLength ? str : str.substring(0, maxLength);
    }
}