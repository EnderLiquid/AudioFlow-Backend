package top.enderliquid.audioflow.service;

import jakarta.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;
import top.enderliquid.audioflow.dto.response.checkin.CheckinResultVO;
import top.enderliquid.audioflow.dto.response.checkin.CheckinStatusVO;
import top.enderliquid.audioflow.dto.response.checkin.CheckinSummaryVO;

@Validated
public interface CheckinService {
    CheckinResultVO checkin(@NotNull(message = "用户ID不能为空") Long userId);

    CheckinStatusVO getTodayStatus(@NotNull(message = "用户ID不能为空") Long userId);

    CheckinSummaryVO getSummary(@NotNull(message = "用户ID不能为空") Long userId);
}