package top.enderliquid.audioflow.config;

import cn.dev33.satoken.stp.StpUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties")
@Slf4j
public abstract class BaseControllerTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @BeforeEach
    void clearSession() {
        try {
            StpUtil.logout();
        } catch (Exception e) {
        }
    }

    protected void mockLogin(Long userId) {
        StpUtil.login(userId);
        log.debug("模拟登录用户ID: {}", userId);
    }

    protected void mockAdminLogin(Long userId) {
        StpUtil.login(userId);
        StpUtil.getSession().set("role", "ADMIN");
        log.debug("模拟管理员登录用户ID: {}", userId);
    }

    protected <T> String toJson(T obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("JSON序列化失败", e);
        }
    }
}
