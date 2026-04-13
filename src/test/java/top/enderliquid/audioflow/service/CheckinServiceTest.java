package top.enderliquid.audioflow.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import top.enderliquid.audioflow.common.TestDataHelper;
import top.enderliquid.audioflow.dto.response.checkin.CheckinStatusVO;
import top.enderliquid.audioflow.dto.response.checkin.CheckinSummaryVO;
import top.enderliquid.audioflow.dto.response.checkin.CheckinVO;
import top.enderliquid.audioflow.entity.CheckinLog;
import top.enderliquid.audioflow.entity.CheckinSummary;
import top.enderliquid.audioflow.entity.User;
import top.enderliquid.audioflow.manager.CheckinLogManager;
import top.enderliquid.audioflow.manager.CheckinSummaryManager;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class CheckinServiceTest {

    @Autowired
    private CheckinService checkinService;

    @Autowired
    private TestDataHelper testDataHelper;

    @Autowired
    private CheckinLogManager checkinLogManager;

    @Autowired
    private CheckinSummaryManager checkinSummaryManager;

    @BeforeEach
    void setUp() {
        testDataHelper.cleanAll();
    }

    @Test
    void testCheckin() {
        User user = testDataHelper.createTestUser();
        Long userId = user.getId();

        CheckinVO result = checkinService.checkin(userId);

        assertNotNull(result);
        assertTrue(result.getRewardPoints() >= 3 && result.getRewardPoints() <= 100);
        assertEquals(1, result.getTotalDays());
        assertEquals(1, result.getContinuousDays());
        assertEquals(1, result.getMaxContinuous());
    }

    @Test
    void testGetTodayStatus() {
        User user = testDataHelper.createTestUser();
        Long userId = user.getId();

        CheckinStatusVO status = checkinService.getTodayStatus(userId);

        assertNotNull(status);
        assertNotNull(status.getCheckedIn());
        assertFalse(status.getCheckedIn());
    }

    @Test
    void testGetSummary() {
        User user = testDataHelper.createTestUser();
        Long userId = user.getId();

        CheckinSummaryVO summary = checkinService.getSummary(userId);

        assertNotNull(summary);
        assertTrue(summary.getTotalDays() >= 0);
        assertTrue(summary.getContinuousDays() >= 0);
        assertTrue(summary.getMaxContinuous() >= 0);
    }

    @Test
    void testDuplicateCheckinSameDay() {
        User user = testDataHelper.createTestUser();
        Long userId = user.getId();

        // 第一次签到成功
        CheckinVO result1 = checkinService.checkin(userId);
        assertNotNull(result1);
        assertEquals(1, result1.getTotalDays());

        // 第二次签到应该抛出异常
        assertThrows(Exception.class, () -> checkinService.checkin(userId));
    }

    @Test
    void testContinuousCheckin() {
        User user = testDataHelper.createTestUser();
        Long userId = user.getId();

        // 模拟昨天签到
        LocalDate yesterday = LocalDate.now().minusDays(1);
        CheckinLog yesterdayLog = new CheckinLog();
        yesterdayLog.setUserId(userId);
        yesterdayLog.setCheckinDate(yesterday);
        yesterdayLog.setRewardPoints(10);
        checkinLogManager.save(yesterdayLog);

        // 创建昨天的签到汇总
        CheckinSummary summary = new CheckinSummary();
        summary.setUserId(userId);
        summary.setTotalDays(1);
        summary.setContinuousDays(1);
        summary.setMaxContinuous(1);
        summary.setLastCheckinDate(yesterday);
        checkinSummaryManager.save(summary);

        // 今天再次签到，应该是连续签到
        CheckinVO result = checkinService.checkin(userId);

        assertNotNull(result);
        assertEquals(2, result.getTotalDays());
        assertEquals(2, result.getContinuousDays());
        assertEquals(2, result.getMaxContinuous());
    }

    @Test
    void testBrokenContinuousCheckin() {
        User user = testDataHelper.createTestUser();
        Long userId = user.getId();

        // 模拟前天签到（跳过了昨天）
        LocalDate twoDaysAgo = LocalDate.now().minusDays(2);
        CheckinLog oldLog = new CheckinLog();
        oldLog.setUserId(userId);
        oldLog.setCheckinDate(twoDaysAgo);
        oldLog.setRewardPoints(10);
        checkinLogManager.save(oldLog);

        // 创建前天的签到汇总
        CheckinSummary summary = new CheckinSummary();
        summary.setUserId(userId);
        summary.setTotalDays(1);
        summary.setContinuousDays(1);
        summary.setMaxContinuous(1);
        summary.setLastCheckinDate(twoDaysAgo);
        checkinSummaryManager.save(summary);

        // 今天签到，由于昨天未签到，连续天数应该重置为1
        CheckinVO result = checkinService.checkin(userId);

        assertNotNull(result);
        assertEquals(2, result.getTotalDays());
        assertEquals(1, result.getContinuousDays()); // 断签后重置
        assertEquals(1, result.getMaxContinuous()); // 最大连续天数保持为1
    }
}