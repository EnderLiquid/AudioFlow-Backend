package top.enderliquid.audioflow.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
// 最大长度: 20
public enum SongStatus {
    UPLOADING,
    DELETING,
    NORMAL
}