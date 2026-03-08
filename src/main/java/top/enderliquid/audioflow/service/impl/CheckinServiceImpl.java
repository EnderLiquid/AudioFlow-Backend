package top.enderliquid.audioflow.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import top.enderliquid.audioflow.common.exception.BusinessException;
import top.enderliquid.audioflow.common.transaction.TransactionHelper;
import top.enderliquid.audioflow.config.CheckinRewardConfig;
import top.enderliquid.audioflow.dto.response.checkin.CheckinResultVO;
import top.enderliquid.audioflow.dto.response.checkin.CheckinStatusVO;
import top.enderliquid.audioflow.dto.response.checkin.CheckinSummaryVO;
import top.enderliquid.audioflow.entity.CheckinLog;
import top.enderliquid.audioflow.entity.CheckinSummary;
import top.enderliquid.audioflow.manager.CheckinLogManager;
import top.enderliquid.audioflow.manager.CheckinSummaryManager;
import top.enderliquid.audioflow.manager.PointsRecordManager;
import top.enderliquid.audioflow.manager.UserManager;
import top.enderliquid.audioflow.service.CheckinService;

import java.time.LocalDate;

import static top.enderliquid.audioflow.common.enums.PointsType.USER_CHECKIN;

@Slf4j
@Service
public class CheckinServiceImpl implements CheckinService {

    @Autowired
    private CheckinLogManager checkinLogManager;

    @Autowired
    private CheckinSummaryManager checkinSummaryManager;

    @Autowired
    private UserManager userManager;

    @Autowired
    private PointsRecordManager pointsRecordManager;

    @Autowired
    private CheckinRewardConfig checkinRewardConfig;

    @Autowired
    private PlatformTransactionManager txManager;

    @Override
    public CheckinResultVO checkin(Long userId) {
        log.info("请求签到，用户ID: {}", userId);

        LocalDate today = LocalDate.now();

        if (checkinLogManager.existsByUserIdAndDate(userId, today)) {
            throw new BusinessException("今日已签到");
        }

        if (!userManager.existsById(userId)) {
            throw new BusinessException("用户不存在");
        }

        int rewardPoints = checkinRewardConfig.getRandomReward();

        CheckinSummary summary;
        try (TransactionHelper tx = new TransactionHelper(txManager)) {
            CheckinLog checkinLog = new CheckinLog();
            checkinLog.setUserId(userId);
            checkinLog.setCheckinDate(today);
            checkinLog.setRewardPoints(rewardPoints);
            checkinLogManager.save(checkinLog);

            summary = updateCheckinSummary(userId, today);

            userManager.addPoints(userId, rewardPoints);

            int balance = userManager.getById(userId).getPoints();
            pointsRecordManager.save(
                userId,
                rewardPoints,
                balance,
                USER_CHECKIN,
                checkinLog.getId()
            );

            tx.commit();
        }

        log.info("签到成功，获得积分: {}", rewardPoints);

        return new CheckinResultVO(
            rewardPoints,
            summary.getTotalDays(),
            summary.getContinuousDays(),
            summary.getMaxContinuous()
        );
    }

    @Override
    public CheckinStatusVO getTodayStatus(Long userId) {
        log.info("查询今日签到状态，用户ID: {}", userId);
        LocalDate today = LocalDate.now();
        boolean checkedIn = checkinLogManager.existsByUserIdAndDate(userId, today);
        log.info("今日签到状态: {}", checkedIn ? "已签到" : "未签到");
        return new CheckinStatusVO(checkedIn);
    }

    @Override
    public CheckinSummaryVO getSummary(Long userId) {
        log.info("查询签到统计，用户ID: {}", userId);
        CheckinSummary summary = checkinSummaryManager.getByUserId(userId);

        if (summary == null) {
            log.info("签到统计为空，返回默认值");
            return new CheckinSummaryVO(0, 0, 0);
        }

        log.info("签到统计查询成功");
        return new CheckinSummaryVO(
            summary.getTotalDays(),
            summary.getContinuousDays(),
            summary.getMaxContinuous()
        );
    }

    private CheckinSummary updateCheckinSummary(Long userId, LocalDate checkinDate) {
        CheckinSummary summary = checkinSummaryManager.getByUserId(userId);

        if (summary == null) {
            summary = new CheckinSummary();
            summary.setUserId(userId);
            summary.setTotalDays(1);
            summary.setContinuousDays(1);
            summary.setMaxContinuous(1);
            summary.setLastCheckinDate(checkinDate);
            checkinSummaryManager.save(summary);
            return summary;
        }

        LocalDate lastCheckinDate = summary.getLastCheckinDate();
        int continuousDays = summary.getContinuousDays();

        if (lastCheckinDate != null && lastCheckinDate.plusDays(1).equals(checkinDate)) {
            continuousDays++;
        } else {
            continuousDays = 1;
        }

        summary.setTotalDays(summary.getTotalDays() + 1);
        summary.setContinuousDays(continuousDays);

        if (continuousDays > summary.getMaxContinuous()) {
            summary.setMaxContinuous(continuousDays);
        }

        summary.setLastCheckinDate(checkinDate);
        checkinSummaryManager.updateById(summary);

        return summary;
    }
}