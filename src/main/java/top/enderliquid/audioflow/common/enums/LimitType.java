package top.enderliquid.audioflow.common.enums;

import lombok.Getter;

@Getter
public enum LimitType {
    IP,      // IP 维度的限流
    USER,    // 用户维度的限流
    GLOBAL   // 接口总访问限制（不区分 IP/用户）
}
