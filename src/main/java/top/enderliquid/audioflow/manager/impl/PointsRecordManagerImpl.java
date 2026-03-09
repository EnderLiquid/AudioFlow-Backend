package top.enderliquid.audioflow.manager.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Repository;
import top.enderliquid.audioflow.common.enums.PointsType;
import top.enderliquid.audioflow.entity.PointsRecord;
import top.enderliquid.audioflow.manager.PointsRecordManager;
import top.enderliquid.audioflow.mapper.PointsRecordMapper;

@Repository
public class PointsRecordManagerImpl extends ServiceImpl<PointsRecordMapper, PointsRecord> implements PointsRecordManager {
    @Override
    public void addRecord(Long userId, int delta, int balance, PointsType type, Long refId) {
        PointsRecord record = new PointsRecord();
        record.setUserId(userId);
        record.setDelta(delta);
        record.setBalance(balance);
        record.setType(type);
        record.setRefId(refId);
        super.save(record);
    }
}