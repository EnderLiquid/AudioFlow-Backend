package top.enderliquid.audioflow.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import top.enderliquid.audioflow.common.TestDataHelper;
import top.enderliquid.audioflow.config.BaseControllerTest;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class UserControllerTest extends BaseControllerTest {

    @Autowired
    private TestDataHelper testDataHelper;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        testDataHelper.cleanDatabase();
    }

    @Test
    void shouldRegisterSuccessfullyWhenEmailNotExists() throws Exception {
        String nickname = "new_user";
        String email = "new_user@example.com";
        String password = "new_password_123";

        Map<String, String> requestDto = new HashMap<>();
        requestDto.put("name", nickname);
        requestDto.put("email", email);
        requestDto.put("password", password);
        String requestJson = objectMapper.writeValueAsString(requestDto);

        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value(email))
                .andExpect(jsonPath("$.message").value("注册成功"));
    }

    @Test
    void shouldReturnErrorWhenEmailAlreadyExists() throws Exception {
        var user = testDataHelper.createTestUser();
        String email = user.getEmail();
        Map<String, String> requestDto = new HashMap<>();
        requestDto.put("name", "another_user");
        requestDto.put("email", email);
        requestDto.put("password", "password");
        String requestJson = objectMapper.writeValueAsString(requestDto);

        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(containsString("邮箱")));
    }

    @Test
    void shouldReturnErrorWhenEmailInvalid() throws Exception {
        Map<String, String> requestDto = new HashMap<>();
        requestDto.put("name", "user");
        requestDto.put("email", "invalid-email");
        requestDto.put("password", "password");
        String requestJson = objectMapper.writeValueAsString(requestDto);

        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(containsString("邮箱")));
    }

    @Test
    void shouldReturnUserInfoWhenLoggedIn() throws Exception {
        var user = testDataHelper.createTestUser();
        String email = user.getEmail();
        String password = "test_password_123";

        Map<String, String> loginDto = new HashMap<>();
        loginDto.put("email", email);
        loginDto.put("password", password);
        String loginJson = objectMapper.writeValueAsString(loginDto);

        var result = mockMvc.perform(post("/api/sessions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn();

        String cookie = result.getResponse().getCookie("satoken").getValue();

        mockMvc.perform(get("/api/users/me")
                .cookie(new org.springframework.mock.web.MockCookie("satoken", cookie)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(user.getId()))
                .andExpect(jsonPath("$.data.email").value(user.getEmail()));
    }

    @Test
    void shouldReturnErrorWhenGetUserWithoutLogin() throws Exception {
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().is(401))
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void shouldUpdatePasswordSuccessfullyWhenCorrectOldPassword() throws Exception {
        var user = testDataHelper.createTestUser();
        String email = user.getEmail();
        String password = "test_password_123";

        Map<String, String> loginDto = new HashMap<>();
        loginDto.put("email", email);
        loginDto.put("password", password);
        String loginJson = objectMapper.writeValueAsString(loginDto);

        var result = mockMvc.perform(post("/api/sessions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn();

        String cookie = result.getResponse().getCookie("satoken").getValue();

        String oldPassword = "test_password_123";
        String newPassword = "new_password_456";

        Map<String, String> passwordDto = new HashMap<>();
        passwordDto.put("oldPassword", oldPassword);
        passwordDto.put("newPassword", newPassword);
        String passwordJson = objectMapper.writeValueAsString(passwordDto);

        mockMvc.perform(patch("/api/users/me/password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(passwordJson)
                .cookie(new org.springframework.mock.web.MockCookie("satoken", cookie)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("密码修改成功，请重新登录"));
    }

    @Test
    void shouldReturnErrorWhenOldPasswordIncorrect() throws Exception {
        var user = testDataHelper.createTestUser();
        String email = user.getEmail();
        String password = "test_password_123";

        Map<String, String> loginDto = new HashMap<>();
        loginDto.put("email", email);
        loginDto.put("password", password);
        String loginJson = objectMapper.writeValueAsString(loginDto);

        var result = mockMvc.perform(post("/api/sessions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn();

        String cookie = result.getResponse().getCookie("satoken").getValue();

        String wrongOldPassword = "wrong_password";
        String newPassword = "new_password_456";

        Map<String, String> passwordDto = new HashMap<>();
        passwordDto.put("oldPassword", wrongOldPassword);
        passwordDto.put("newPassword", newPassword);
        String passwordJson = objectMapper.writeValueAsString(passwordDto);

        mockMvc.perform(patch("/api/users/me/password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(passwordJson)
                .cookie(new org.springframework.mock.web.MockCookie("satoken", cookie)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(containsString("密码")));
    }

    @Test
    void shouldReturnErrorWhenUpdatePasswordWithoutLogin() throws Exception {
        Map<String, String> passwordDto = new HashMap<>();
        passwordDto.put("oldPassword", "old");
        passwordDto.put("newPassword", "new");
        String passwordJson = objectMapper.writeValueAsString(passwordDto);

        mockMvc.perform(patch("/api/users/me/password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(passwordJson))
                .andExpect(status().is(401))
                .andExpect(jsonPath("$.success").value(false));
    }
}
