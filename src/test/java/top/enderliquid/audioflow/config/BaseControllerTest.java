package top.enderliquid.audioflow.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import top.enderliquid.audioflow.common.MockOSSConfig;
import top.enderliquid.audioflow.common.TestDataHelper;

@SpringBootTest
@AutoConfigureMockMvc
@Import(MockOSSConfig.class)
public abstract class BaseControllerTest {
    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected TestDataHelper testDataHelper;

    @BeforeEach
    void clearSession() {
        testDataHelper.cleanAll();
    }
}
