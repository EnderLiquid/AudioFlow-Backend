package top.enderliquid.audioflow.manager.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import top.enderliquid.audioflow.entity.CheckinSummary;
import top.enderliquid.audioflow.manager.CheckinSummaryManager;
import top.enderliquid.audioflow.mapper.CheckinSummaryMapper;

@Repository
@RequiredArgsConstructor
public class CheckinSummaryManagerImpl extends ServiceImpl<CheckinSummaryMapper, CheckinSummary> implements CheckinSummaryManager {
    private final CheckinSummaryMapper checkinSummaryMapper;

    @Override
    public CheckinSummary getByUserId(Long userId) {
        return getById(userId);
    }

    @Override
    public boolean existsByUserId(Long userId) {
        LambdaQueryWrapper<CheckinSummary> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CheckinSummary::getUserId, userId);
        return exists(wrapper);
    }

    @Override
    public CheckinSummary getByUserIdForUpdate(Long userId) {
        return checkinSummaryMapper.getByUserIdForUpdate(userId);
    }
}