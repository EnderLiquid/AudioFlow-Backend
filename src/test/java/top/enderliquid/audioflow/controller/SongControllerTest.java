package top.enderliquid.audioflow.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.multipart.MultipartFile;
import top.enderliquid.audioflow.common.TestDataHelper;
import top.enderliquid.audioflow.config.BaseControllerTest;
import top.enderliquid.audioflow.entity.Song;
import top.enderliquid.audioflow.entity.User;
import top.enderliquid.audioflow.manager.UserManager;

import java.io.InputStream;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class SongControllerTest extends BaseControllerTest {

    @Autowired
    protected TestDataHelper testDataHelper;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected UserManager userManager;

    protected User testUser;
    protected Song testSong;

    @BeforeEach
    void setUp() {
        testDataHelper.cleanDatabase();
        testUser = testDataHelper.createTestUser();
        testSong = testDataHelper.createTestSong(testUser.getId());
    }

    @Test
    void shouldReturnSongPageWhenQueryWithPagination() throws Exception {
        mockMvc.perform(get("/api/songs")
                .param("pageIndex", "1")
                .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.list").isArray());
    }

    @Test
    void shouldReturnEmptyListWhenNoSongs() throws Exception {
        testDataHelper.cleanDatabase();

        mockMvc.perform(get("/api/songs")
                .param("pageIndex", "1")
                .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.list").isArray());
    }

    @Test
    void shouldReturnSongInfoWhenSongExists() throws Exception {
        mockMvc.perform(get("/api/songs/{id}", testSong.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(testSong.getId()))
                .andExpect(jsonPath("$.data.name").value(testSong.getName()));
    }

    @Test
    void shouldReturnErrorWhenSongNotExists() throws Exception {
        Long nonExistentId = 999999L;

        mockMvc.perform(get("/api/songs/{id}", nonExistentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("不存在")));
    }

    @Test
    void shouldRedirectWhenGetSongPlayUrl() throws Exception {
        var result = mockMvc.perform(get("/api/songs/{id}/play", testSong.getId()))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().exists("Location"))
                .andReturn();

        String location = result.getResponse().getHeader("Location");
        assertTrue(location != null && !location.isEmpty());
    }

    @Test
    void shouldReturn404WhenSongNotExistsPlayUrl() throws Exception {
        Long nonExistentId = 999999L;

        mockMvc.perform(get("/api/songs/{id}/play", nonExistentId))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldDeleteSuccessfullyWhenOwner() throws Exception {
        String email = testUser.getEmail();
        String password = "test_password_123";

        var loginDto = new java.util.HashMap<String, String>();
        loginDto.put("email", email);
        loginDto.put("password", password);
        String loginJson = objectMapper.writeValueAsString(loginDto);

        var result = mockMvc.perform(post("/api/sessions")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content(loginJson))
                .andExpect(status().isOk())
                .andReturn();

        String cookie = result.getResponse().getCookie("satoken").getValue();

        mockMvc.perform(delete("/api/songs/{id}", testSong.getId())
                .cookie(new org.springframework.mock.web.MockCookie("satoken", cookie)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("删除成功"));
    }

    @Test
    void shouldReturnErrorWhenDeleteWithoutLogin() throws Exception {
        mockMvc.perform(delete("/api/songs/{id}", testSong.getId()))
                .andExpect(status().is(401))
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void shouldReturnErrorWhenDeleteByNonOwner() throws Exception {
        User anotherUser = testDataHelper.createTestUser();
        String email = anotherUser.getEmail();
        String password = "test_password_123";

        var loginDto = new java.util.HashMap<String, String>();
        loginDto.put("email", email);
        loginDto.put("password", password);
        String loginJson = objectMapper.writeValueAsString(loginDto);

        var result = mockMvc.perform(post("/api/sessions")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content(loginJson))
                .andExpect(status().isOk())
                .andReturn();

        String cookie = result.getResponse().getCookie("satoken").getValue();

        mockMvc.perform(delete("/api/songs/{id}", testSong.getId())
                .cookie(new org.springframework.mock.web.MockCookie("satoken", cookie)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void shouldUploadSuccessfullyWhenLoggedIn() throws Exception {
        String email = testUser.getEmail();
        String password = "test_password_123";

        var loginDto = new java.util.HashMap<String, String>();
        loginDto.put("email", email);
        loginDto.put("password", password);
        String loginJson = objectMapper.writeValueAsString(loginDto);

        var result = mockMvc.perform(post("/api/sessions")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content(loginJson))
                .andExpect(status().isOk())
                .andReturn();

        String cookie = result.getResponse().getCookie("satoken").getValue();

        org.springframework.core.io.ClassPathResource audioResource = 
            new org.springframework.core.io.ClassPathResource("audio/test-song.mp3");
        var audioFile = audioResource.getFile();

        MockMultipartFile file = new MockMultipartFile(
            "file",
            "test-song.mp3",
            "audio/mpeg",
            Files.readAllBytes(audioFile.toPath())
        );

        mockMvc.perform(multipart("/api/songs")
                .file(file)
                .param("name", "New Song")
                .param("description", "New Description")
                .cookie(new org.springframework.mock.web.MockCookie("satoken", cookie)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("上传成功"));
    }

    @Test
    void shouldReturnErrorWhenUploadWithoutLogin() throws Exception {
        org.springframework.core.io.ClassPathResource audioResource = 
            new org.springframework.core.io.ClassPathResource("audio/test-song.mp3");
        var audioFile = audioResource.getFile();

        MockMultipartFile file = new MockMultipartFile(
            "file",
            "test-song.mp3",
            "audio/mpeg",
            Files.readAllBytes(audioFile.toPath())
        );

        mockMvc.perform(multipart("/api/songs")
                .file(file)
                .param("name", "New Song")
                .param("description", "New Description"))
                .andExpect(status().is(401))
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void shouldUpdateSuccessfullyWhenOwner() throws Exception {
        String email = testUser.getEmail();
        String password = "test_password_123";

        var loginDto = new java.util.HashMap<String, String>();
        loginDto.put("email", email);
        loginDto.put("password", password);
        String loginJson = objectMapper.writeValueAsString(loginDto);

        var result = mockMvc.perform(post("/api/sessions")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content(loginJson))
                .andExpect(status().isOk())
                .andReturn();

        String cookie = result.getResponse().getCookie("satoken").getValue();

        var updateDto = new java.util.HashMap<String, Object>();
        updateDto.put("songId", testSong.getId());
        updateDto.put("name", "Updated Title");
        updateDto.put("description", "Updated Description");
        String updateJson = objectMapper.writeValueAsString(updateDto);

        mockMvc.perform(patch("/api/songs/{id}", testSong.getId())
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content(updateJson)
                .cookie(new org.springframework.mock.web.MockCookie("satoken", cookie)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Updated Title"));
    }

    @Test
    void shouldReturnErrorWhenUpdateWithoutLogin() throws Exception {
        var updateDto = new java.util.HashMap<String, Object>();
        updateDto.put("songId", testSong.getId());
        updateDto.put("name", "Updated Title");
        String updateJson = objectMapper.writeValueAsString(updateDto);

        mockMvc.perform(patch("/api/songs/{id}", testSong.getId())
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content(updateJson))
                .andExpect(status().is(401))
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void shouldReturnErrorWhenUpdateByNonOwner() throws Exception {
        User anotherUser = testDataHelper.createTestUser();
        String email = anotherUser.getEmail();
        String password = "test_password_123";

        var loginDto = new java.util.HashMap<String, String>();
        loginDto.put("email", email);
        loginDto.put("password", password);
        String loginJson = objectMapper.writeValueAsString(loginDto);

        var result = mockMvc.perform(post("/api/sessions")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content(loginJson))
                .andExpect(status().isOk())
                .andReturn();

        String cookie = result.getResponse().getCookie("satoken").getValue();

        var updateDto = new java.util.HashMap<String, Object>();
        updateDto.put("songId", testSong.getId());
        updateDto.put("name", "Updated Title");
        String updateJson = objectMapper.writeValueAsString(updateDto);

        mockMvc.perform(patch("/api/songs/{id}", testSong.getId())
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content(updateJson)
                .cookie(new org.springframework.mock.web.MockCookie("satoken", cookie)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void shouldReturnErrorWhenForceDeleteByNonAdmin() throws Exception {
        String email = testUser.getEmail();
        String password = "test_password_123";

        var loginDto = new java.util.HashMap<String, String>();
        loginDto.put("email", email);
        loginDto.put("password", password);
        String loginJson = objectMapper.writeValueAsString(loginDto);

        var result = mockMvc.perform(post("/api/sessions")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content(loginJson))
                .andExpect(status().isOk())
                .andReturn();

        String cookie = result.getResponse().getCookie("satoken").getValue();

        mockMvc.perform(delete("/api/songs/{id}/force", testSong.getId())
                .cookie(new org.springframework.mock.web.MockCookie("satoken", cookie)))
                .andExpect(status().is(403));
    }

    @Test
    void shouldReturnErrorWhenForceUpdateByNonAdmin() throws Exception {
        String email = testUser.getEmail();
        String password = "test_password_123";

        var loginDto = new java.util.HashMap<String, String>();
        loginDto.put("email", email);
        loginDto.put("password", password);
        String loginJson = objectMapper.writeValueAsString(loginDto);

        var result = mockMvc.perform(post("/api/sessions")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content(loginJson))
                .andExpect(status().isOk())
                .andReturn();

        String cookie = result.getResponse().getCookie("satoken").getValue();

        var updateDto = new java.util.HashMap<String, Object>();
        updateDto.put("songId", testSong.getId());
        updateDto.put("name", "Force Updated Title");
        String updateJson = objectMapper.writeValueAsString(updateDto);

        mockMvc.perform(patch("/api/songs/{id}/force", testSong.getId())
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content(updateJson)
                .cookie(new org.springframework.mock.web.MockCookie("satoken", cookie)))
                .andExpect(status().is(403));
    }
}
