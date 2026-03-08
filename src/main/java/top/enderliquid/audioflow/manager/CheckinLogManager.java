package top.enderliquid.audioflow.manager;

import com.baomidou.mybatisplus.extension.service.IService;
import top.enderliquid.audioflow.entity.CheckinLog;

import java.time.LocalDate;

public interface CheckinLogManager extends IService<CheckinLog> {
    boolean existsByUserIdAndDate(Long userId, LocalDate date);

    CheckinLog getByUserIdAndDate(Long userId, LocalDate date);
}