package top.enderliquid.audioflow.dto.request.session;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 登录上下文DTO，封装设备信息
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class LoginContext {
    // IP地址
    private String ip;
    // 设备类型
    private String deviceType;
    // 操作系统
    private String os;
    // 浏览器
    private String browser;
    // 完整的 User-Agent
    private String userAgent;
}