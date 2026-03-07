package top.enderliquid.audioflow.manager;

import com.baomidou.mybatisplus.extension.service.IService;
import top.enderliquid.audioflow.common.enums.PointsType;
import top.enderliquid.audioflow.entity.PointsRecord;

public interface PointsRecordManager extends IService<PointsRecord> {
    void save(Long userId, int delta, int balance, PointsType type, Long refId);
}