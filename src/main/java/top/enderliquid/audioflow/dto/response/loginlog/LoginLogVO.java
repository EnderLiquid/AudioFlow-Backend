package top.enderliquid.audioflow.dto.response.loginlog;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 登录流水响应VO
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class LoginLogVO {
    // IP地址
    private String ip;
    // 设备类型
    private String deviceType;
    // 操作系统
    private String os;
    // 浏览器
    private String browser;
    // 创建时间
    private LocalDateTime createTime;
}