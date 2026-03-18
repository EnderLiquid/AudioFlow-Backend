package top.enderliquid.audioflow.manager;

import com.baomidou.mybatisplus.extension.service.IService;
import top.enderliquid.audioflow.entity.CheckinLog;

import java.time.LocalDate;

public interface CheckinLogManager extends IService<CheckinLog> {
    boolean existsByUserIdAndDate(Long userId, LocalDate date);

    CheckinLog getByUserIdAndDate(Long userId, LocalDate date);

    /**
     * 添加签到流水记录
     *
     * @param userId       用户ID
     * @param checkinDate  签到日期
     * @param rewardPoints 奖励积分
     */
    void addRecord(Long userId, LocalDate checkinDate, Integer rewardPoints);
}