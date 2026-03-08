package top.enderliquid.audioflow.manager;

import com.baomidou.mybatisplus.extension.service.IService;
import top.enderliquid.audioflow.entity.CheckinSummary;

public interface CheckinSummaryManager extends IService<CheckinSummary> {
    CheckinSummary getByUserId(Long userId);

    boolean existsByUserId(Long userId);

    CheckinSummary getByUserIdForUpdate(Long userId);
}