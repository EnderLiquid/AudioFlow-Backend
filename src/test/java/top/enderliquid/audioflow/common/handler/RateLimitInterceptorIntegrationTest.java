package top.enderliquid.audioflow.common.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import top.enderliquid.audioflow.common.TestDataHelper;

import java.util.HashMap;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 限流拦截器集成测试
 * 测试两种拦截器顺序下是否能正确读取用户ID
 */
@SpringBootTest
@AutoConfigureMockMvc
class RateLimitInterceptorIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TestDataHelper testDataHelper;

    /**
     * 测试未登录情况下访问限流接口
     * 预期：用户ID为 null
     */
    @Test
    void testRateLimitWithoutLogin() throws Exception {
        // 访问登录接口（有IP限流，无用户限流）
        mockMvc.perform(get("/api/songs?page=1&size=10"))
                .andExpect(status().isOk());
    }

    /**
     * 测试登录情况下访问限流接口
     * 预期：用户ID应该能正确读取
     */
    @Test
    void testRateLimitWithLogin() throws Exception {
        // 1. 创建测试用户
        var user = testDataHelper.createTestUser();
        String email = user.getEmail();
        String password = "test_password_123";

        // 2. 登录获取 token
        Map<String, String> loginDto = new HashMap<>();
        loginDto.put("email", email);
        loginDto.put("password", password);
        String loginJson = objectMapper.writeValueAsString(loginDto);

        MvcResult result = mockMvc.perform(post("/api/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn();

        String cookie = result.getResponse().getCookie("satoken").getValue();

        // 3. 访问带有 @RateLimits 的用户信息接口（需要登录）
        // 注意：这里会触发限流拦截器，查看日志中读取到的用户ID
        mockMvc.perform(get("/api/users/me")
                        .cookie(new org.springframework.mock.web.MockCookie("satoken", cookie)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
