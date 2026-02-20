package top.enderliquid.audioflow.controller;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import top.enderliquid.audioflow.common.TestDataHelper;
import top.enderliquid.audioflow.config.BaseControllerTest;

class SessionControllerTest extends BaseControllerTest {

    @Autowired
    private TestDataHelper testDataHelper;

    @BeforeEach
    void setUp() {
        testDataHelper.cleanDatabase();
    }
}
