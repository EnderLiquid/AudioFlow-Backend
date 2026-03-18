package top.enderliquid.audioflow.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import top.enderliquid.audioflow.common.enums.LoginFailReason;

import java.time.LocalDateTime;

/**
 * 登录流水实体
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
@TableName("login_log")
public class LoginLog {
    @TableId
    private Long id;
    // 用户ID，账号不存在时为null
    private Long userId;
    // 尝试登录的邮箱
    private String email;
    // 是否登录成功
    private boolean success;
    // 失败原因
    private LoginFailReason failReason;
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
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}