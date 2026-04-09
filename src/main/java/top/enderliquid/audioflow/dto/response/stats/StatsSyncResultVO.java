package top.enderliquid.audioflow.dto.response.stats;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统计数据同步结果
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class StatsSyncResultVO {
    /**
     * 日活数据同步条数
     */
    private Integer dauSyncCount;

    /**
     * 签到数据同步条数
     */
    private Integer checkinSyncCount;
}