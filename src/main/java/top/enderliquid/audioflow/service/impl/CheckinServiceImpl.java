package top.enderliquid.audioflow.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
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
import top.enderliquid.audioflow.manager.*;
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

    @Autowired
    private CheckinCountManager checkinCountManager;

    @Override
    public CheckinResultVO checkin(Long userId) {
        log.info("请求签到，用户ID: {}", userId);

        LocalDate today = LocalDate.now();

        // 初步判断是否签到，快速失败
        if (checkinLogManager.existsByUserIdAndDate(userId, today)) {
            throw new BusinessException("今日已签到");
        }

        if (!userManager.existsById(userId)) {
            throw new BusinessException("用户不存在");
        }

        int rewardPoints = checkinRewardConfig.getRandomReward();
        CheckinSummary summary;

        try (TransactionHelper tx = new TransactionHelper(txManager)) {

            /*
              利用悲观锁锁住当前用户的汇总记录，
              解决跨天并发导致的数据相互覆盖问题，
              其他并发的签到线程会在这里阻塞排队。
            */
            summary = checkinSummaryManager.getByUserIdForUpdate(userId);

            if (summary == null) {
                // 首次签到时的初始化逻辑
                summary = initCheckinSummary(userId);
                try {
                    // 依赖 checkin_summary 表中 user_id 的唯一索引来防止并发插入
                    checkinSummaryManager.save(summary);
                } catch (DuplicateKeyException e) {
                    // 如果抛出唯一键冲突异常，说明别的并发请求抢先初始化并提交了
                    throw new BusinessException("签到初始化失败，请重试");
                }
            }

            // 再次校验今日是否已签到
            if (summary.getLastCheckinDate() != null && summary.getLastCheckinDate().equals(today)) {
                throw new BusinessException("今日已签到");
            }

            CheckinLog checkinLog = new CheckinLog();
            checkinLog.setUserId(userId);
            checkinLog.setCheckinDate(today);
            checkinLog.setRewardPoints(rewardPoints);
            try {
                checkinLogManager.save(checkinLog);
            } catch (DuplicateKeyException e) {
                /*
                  依赖 user_id + checkin_date 联合唯一索引
                  防御性编程兜底，有前面排他锁保障，理论上不可能走到这一个分支
                */
                throw new BusinessException("今日已签到");
            }

            updateSummaryData(summary, today);
            checkinSummaryManager.updateById(summary);

            // 先读后写，拿到行锁，无并发问题
            userManager.addPoints(userId, rewardPoints);
            int balance = userManager.getById(userId).getPoints();
            pointsRecordManager.addRecord(
                    userId,
                    rewardPoints,
                    balance,
                    USER_CHECKIN,
                    checkinLog.getId()
            );

            tx.commit();
        }

        checkinCountManager.incrementCheckinCount(LocalDate.now());

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

    private void updateSummaryData(CheckinSummary summary, LocalDate checkinDate) {
        LocalDate lastCheckinDate = summary.getLastCheckinDate();
        int continuousDays = summary.getContinuousDays();

        if (lastCheckinDate != null && lastCheckinDate.plusDays(1).equals(checkinDate)) {
            continuousDays++;
        } else {
            // 断签或首次签到
            continuousDays = 1;
        }

        summary.setTotalDays(summary.getTotalDays() + 1);
        summary.setContinuousDays(continuousDays);

        if (continuousDays > summary.getMaxContinuous()) {
            summary.setMaxContinuous(continuousDays);
        }

        summary.setLastCheckinDate(checkinDate);
    }

    private CheckinSummary initCheckinSummary(Long userId) {
        CheckinSummary summary = new CheckinSummary();
        summary.setUserId(userId);
        // 初始化为0，外层的 updateSummaryData 会在本次签到后将其加为1
        summary.setTotalDays(0);
        summary.setContinuousDays(0);
        summary.setMaxContinuous(0);
        return summary;
    }
}
