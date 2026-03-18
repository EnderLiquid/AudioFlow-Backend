package top.enderliquid.audioflow.manager.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Repository;
import top.enderliquid.audioflow.entity.CheckinLog;
import top.enderliquid.audioflow.manager.CheckinLogManager;
import top.enderliquid.audioflow.mapper.CheckinLogMapper;

import java.time.LocalDate;

@Repository
public class CheckinLogManagerImpl extends ServiceImpl<CheckinLogMapper, CheckinLog> implements CheckinLogManager {
    @Override
    public boolean existsByUserIdAndDate(Long userId, LocalDate date) {
        LambdaQueryWrapper<CheckinLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CheckinLog::getUserId, userId)
               .eq(CheckinLog::getCheckinDate, date);
        return super.exists(wrapper);
    }

    @Override
    public CheckinLog getByUserIdAndDate(Long userId, LocalDate date) {
        LambdaQueryWrapper<CheckinLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CheckinLog::getUserId, userId)
               .eq(CheckinLog::getCheckinDate, date);
        return super.getOne(wrapper);
    }

    @Override
    public void addRecord(Long userId, LocalDate checkinDate, Integer rewardPoints) {
        CheckinLog checkinLog = new CheckinLog();
        checkinLog.setUserId(userId);
        checkinLog.setCheckinDate(checkinDate);
        checkinLog.setRewardPoints(rewardPoints);
        save(checkinLog);
    }
}