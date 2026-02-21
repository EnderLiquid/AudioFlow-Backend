package top.enderliquid.audioflow.common.enums;

import lombok.Getter;

@Getter
public enum LimitType {
    IP,      // 仅限IP
    USER,    // 仅限账号
    BOTH     // IP和账号双重限制
}
