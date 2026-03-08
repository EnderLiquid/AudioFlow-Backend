package top.enderliquid.audioflow.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import top.enderliquid.audioflow.common.TestDataHelper;
import top.enderliquid.audioflow.dto.response.checkin.CheckinResultVO;
import top.enderliquid.audioflow.dto.response.checkin.CheckinStatusVO;
import top.enderliquid.audioflow.dto.response.checkin.CheckinSummaryVO;
import top.enderliquid.audioflow.entity.User;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class CheckinServiceTest {

    @Autowired
    private CheckinService checkinService;

    @Autowired
    private TestDataHelper testDataHelper;

    @BeforeEach
    void setUp() {
        testDataHelper.cleanAll();
    }

    @Test
    void testCheckin() {
        User user = testDataHelper.createTestUser();
        Long userId = user.getId();

        CheckinResultVO result = checkinService.checkin(userId);

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
}