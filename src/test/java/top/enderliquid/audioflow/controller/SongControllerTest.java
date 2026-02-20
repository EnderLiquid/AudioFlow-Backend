package top.enderliquid.audioflow.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import top.enderliquid.audioflow.common.TestDataHelper;
import top.enderliquid.audioflow.config.BaseControllerTest;
import top.enderliquid.audioflow.entity.User;
import top.enderliquid.audioflow.entity.Song;

class SongControllerTest extends BaseControllerTest {

    @Autowired
    protected TestDataHelper testDataHelper;

    @Autowired
    protected ObjectMapper objectMapper;

    protected User testUser;
    protected Song testSong;

    @BeforeEach
    void setUp() {
        testDataHelper.cleanDatabase();
        testUser = testDataHelper.createTestUser();
        testSong = testDataHelper.createTestSong(testUser.getId());
    }
}
