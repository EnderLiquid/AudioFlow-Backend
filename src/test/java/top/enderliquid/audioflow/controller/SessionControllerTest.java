package top.enderliquid.audioflow.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import top.enderliquid.audioflow.common.TestDataHelper;
import top.enderliquid.audioflow.config.BaseControllerTest;
import top.enderliquid.audioflow.entity.User;

import java.util.HashMap;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SessionControllerTest extends BaseControllerTest {

    @Autowired
    private TestDataHelper testDataHelper;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        testDataHelper.cleanDatabase();
    }

    @Test
    void shouldLoginSuccessfullyWhenCredentialsCorrect() throws Exception {
        User user = testDataHelper.createTestUser();
        String email = user.getEmail();
        String password = "test_password_123";

        Map<String, String> requestDto = new HashMap<>();
        requestDto.put("email", email);
        requestDto.put("password", password);
        String requestJson = objectMapper.writeValueAsString(requestDto);

        mockMvc.perform(post("/api/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value(email))
                .andExpect(jsonPath("$.message").value("登录成功"));
    }

    @Test
    void shouldReturnErrorWhenPasswordIncorrect() throws Exception {
        User user = testDataHelper.createTestUser();
        String email = user.getEmail();
        String wrongPassword = "wrong_password";

        Map<String, String> requestDto = new HashMap<>();
        requestDto.put("email", email);
        requestDto.put("password", wrongPassword);
        String requestJson = objectMapper.writeValueAsString(requestDto);

        mockMvc.perform(post("/api/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("密码错误"));
    }

    @Test
    void shouldLogoutSuccessfullyWhenLoggedIn() throws Exception {
        User user = testDataHelper.createTestUser();
        String email = user.getEmail();
        String password = "test_password_123";

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

        mockMvc.perform(delete("/api/sessions/current")
                        .cookie(new org.springframework.mock.web.MockCookie("satoken", cookie)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("注销成功"));
    }

    @Test
    void shouldReturnErrorWhenLogoutWithoutLogin() throws Exception {
        mockMvc.perform(delete("/api/sessions/current"))
                .andExpect(status().is(401))
                .andExpect(jsonPath("$.success").value(false));
    }
}
