package top.enderliquid.audioflow.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MvcResult;
import top.enderliquid.audioflow.common.TestDataHelper;
import top.enderliquid.audioflow.entity.Song;
import top.enderliquid.audioflow.entity.User;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class SongControllerTest extends BaseControllerTest {

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected top.enderliquid.audioflow.common.MockOSSConfig.MockOSSManager mockOSSManager;

    protected User testUser;
    protected Song testSong;

    @BeforeEach
    void setUp() {
        testUser = testDataHelper.createTestUser();
        testSong = testDataHelper.createTestSong(testUser.getId());
    }

    @Test
    void shouldReturnSongInfoWhenSongExists() throws Exception {
        mockMvc.perform(get("/api/songs/{songId}", testSong.getId()))
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
        MvcResult result = mockMvc.perform(get("/api/songs/{songId}/play", testSong.getId()))
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

        java.util.HashMap<String, String> loginDto = new java.util.HashMap<>();
        loginDto.put("email", email);
        loginDto.put("password", password);
        String loginJson = objectMapper.writeValueAsString(loginDto);

        MvcResult result = mockMvc.perform(post("/api/sessions")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(loginJson))
                .andExpect(status().isOk())
                .andReturn();

        String cookie = result.getResponse().getCookie("satoken").getValue();

        mockMvc.perform(delete("/api/songs/{songId}", testSong.getId())
                        .cookie(new org.springframework.mock.web.MockCookie("satoken", cookie)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("删除成功"));
    }

    @Test
    void shouldReturnErrorWhenDeleteWithoutLogin() throws Exception {
        mockMvc.perform(delete("/api/songs/{songId}", testSong.getId()))
                .andExpect(status().is(401))
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void shouldUploadSuccessfullyWithNewFlow() throws Exception {
        String email = testUser.getEmail();
        String password = "test_password_123";

        java.util.HashMap<String, String> loginDto = new java.util.HashMap<>();
        loginDto.put("email", email);
        loginDto.put("password", password);
        String loginJson = objectMapper.writeValueAsString(loginDto);

        MvcResult result = mockMvc.perform(post("/api/sessions")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(loginJson))
                .andExpect(status().isOk())
                .andReturn();

        String cookie = result.getResponse().getCookie("satoken").getValue();

        java.util.HashMap<String, Object> prepareDto = new java.util.HashMap<>();
        prepareDto.put("name", "New Song");
        prepareDto.put("description", "New Description");
        prepareDto.put("mimeType", "audio/mpeg");
        prepareDto.put("size", 1024L);
        String prepareJson = objectMapper.writeValueAsString(prepareDto);

        result = mockMvc.perform(post("/api/songs/prepare")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(prepareJson)
                        .cookie(new org.springframework.mock.web.MockCookie("satoken", cookie)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").exists())
                .andExpect(jsonPath("$.data.fileName").exists())
                .andExpect(jsonPath("$.data.uploadUrl").exists())
                .andReturn();

        String response = result.getResponse().getContentAsString();
        com.fasterxml.jackson.databind.JsonNode jsonNode = objectMapper.readTree(response);
        Long songId = jsonNode.get("data").get("id").asLong();
        String fileName = jsonNode.get("data").get("fileName").asText();

        mockOSSManager.simulateUpload(fileName);

        java.util.HashMap<String, Object> completeDto = new java.util.HashMap<>();
        completeDto.put("songId", songId);
        String completeJson = objectMapper.writeValueAsString(completeDto);

        mockMvc.perform(post("/api/songs/complete")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(completeJson)
                        .cookie(new org.springframework.mock.web.MockCookie("satoken", cookie)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void shouldReturnErrorWhenPrepareUploadWithoutLogin() throws Exception {
        java.util.HashMap<String, Object> prepareDto = new java.util.HashMap<>();
        prepareDto.put("name", "New Song");
        prepareDto.put("description", "New Description");
        prepareDto.put("mimeType", "audio/mpeg");
        prepareDto.put("size", 1024L);
        String prepareJson = objectMapper.writeValueAsString(prepareDto);

        mockMvc.perform(post("/api/songs/prepare")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(prepareJson))
                .andExpect(status().is(401))
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void shouldUpdateSuccessfullyWhenOwner() throws Exception {
        String email = testUser.getEmail();
        String password = "test_password_123";

        java.util.HashMap<String, String> loginDto = new java.util.HashMap<>();
        loginDto.put("email", email);
        loginDto.put("password", password);
        String loginJson = objectMapper.writeValueAsString(loginDto);

        MvcResult result = mockMvc.perform(post("/api/sessions")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(loginJson))
                .andExpect(status().isOk())
                .andReturn();

        String cookie = result.getResponse().getCookie("satoken").getValue();

        java.util.HashMap<String, Object> updateDto = new java.util.HashMap<>();
        updateDto.put("name", "Updated Title");
        updateDto.put("description", "Updated Description");
        String updateJson = objectMapper.writeValueAsString(updateDto);

        mockMvc.perform(patch("/api/songs/{songId}", testSong.getId())
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(updateJson)
                        .cookie(new org.springframework.mock.web.MockCookie("satoken", cookie)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Updated Title"));
    }

    @Test
    void shouldReturnErrorWhenUpdateWithoutLogin() throws Exception {
        java.util.HashMap<String, Object> updateDto = new java.util.HashMap<>();
        updateDto.put("name", "Updated Title");
        String updateJson = objectMapper.writeValueAsString(updateDto);

        mockMvc.perform(patch("/api/songs/{songId}", testSong.getId())
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

        java.util.HashMap<String, String> loginDto = new java.util.HashMap<>();
        loginDto.put("email", email);
        loginDto.put("password", password);
        String loginJson = objectMapper.writeValueAsString(loginDto);

        MvcResult result = mockMvc.perform(post("/api/sessions")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(loginJson))
                .andExpect(status().isOk())
                .andReturn();

        String cookie = result.getResponse().getCookie("satoken").getValue();

        java.util.HashMap<String, Object> updateDto = new java.util.HashMap<>();
        updateDto.put("songId", testSong.getId());
        updateDto.put("name", "Updated Title");
        String updateJson = objectMapper.writeValueAsString(updateDto);

        mockMvc.perform(patch("/api/songs/{songId}", testSong.getId())
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(updateJson)
                        .cookie(new org.springframework.mock.web.MockCookie("satoken", cookie)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void shouldBatchPrepareUploadSuccessfully() throws Exception {
        String email = testUser.getEmail();
        String password = "test_password_123";

        java.util.HashMap<String, String> loginDto = new java.util.HashMap<>();
        loginDto.put("email", email);
        loginDto.put("password", password);
        String loginJson = objectMapper.writeValueAsString(loginDto);

        MvcResult result = mockMvc.perform(post("/api/sessions")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(loginJson))
                .andExpect(status().isOk())
                .andReturn();

        String cookie = result.getResponse().getCookie("satoken").getValue();

        java.util.HashMap<String, Object> batchPrepareDto = new java.util.HashMap<>();
        java.util.List<java.util.HashMap<String, Object>> songs = new java.util.ArrayList<>();
        java.util.HashMap<String, Object> song1 = new java.util.HashMap<>();
        song1.put("name", "Batch Song 1");
        song1.put("description", "Batch Description 1");
        song1.put("mimeType", "audio/mpeg");
        song1.put("size", 1024L);
        songs.add(song1);
        java.util.HashMap<String, Object> song2 = new java.util.HashMap<>();
        song2.put("name", "Batch Song 2");
        song2.put("description", "Batch Description 2");
        song2.put("mimeType", "audio/mpeg");
        song2.put("size", 2048L);
        songs.add(song2);
        batchPrepareDto.put("songs", songs);
        String prepareJson = objectMapper.writeValueAsString(batchPrepareDto);

        mockMvc.perform(post("/api/songs/batch-prepare")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(prepareJson)
                        .cookie(new org.springframework.mock.web.MockCookie("satoken", cookie)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.successCount").value(2))
                .andExpect(jsonPath("$.data.successList").isArray());
    }

    @Test
    void shouldBatchPrepareUploadWithoutLogin() throws Exception {
        java.util.HashMap<String, Object> batchPrepareDto = new java.util.HashMap<>();
        java.util.List<java.util.HashMap<String, Object>> songs = new java.util.ArrayList<>();
        java.util.HashMap<String, Object> song1 = new java.util.HashMap<>();
        song1.put("name", "Batch Song 1");
        song1.put("mimeType", "audio/mpeg");
        song1.put("size", 1024L);
        songs.add(song1);
        batchPrepareDto.put("songs", songs);
        String prepareJson = objectMapper.writeValueAsString(batchPrepareDto);

        mockMvc.perform(post("/api/songs/batch-prepare")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(prepareJson))
                .andExpect(status().is(401))
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void shouldBatchCompleteUploadSuccessfully() throws Exception {
        String email = testUser.getEmail();
        String password = "test_password_123";

        java.util.HashMap<String, String> loginDto = new java.util.HashMap<>();
        loginDto.put("email", email);
        loginDto.put("password", password);
        String loginJson = objectMapper.writeValueAsString(loginDto);

        MvcResult result = mockMvc.perform(post("/api/sessions")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(loginJson))
                .andExpect(status().isOk())
                .andReturn();

        String cookie = result.getResponse().getCookie("satoken").getValue();

        java.util.HashMap<String, Object> prepareDto = new java.util.HashMap<>();
        prepareDto.put("name", "Complete Song");
        prepareDto.put("description", "Test Description");
        prepareDto.put("mimeType", "audio/mpeg");
        prepareDto.put("size", 1024L);
        String prepareJson = objectMapper.writeValueAsString(prepareDto);

        result = mockMvc.perform(post("/api/songs/prepare")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(prepareJson)
                        .cookie(new org.springframework.mock.web.MockCookie("satoken", cookie)))
                .andExpect(status().isOk())
                .andReturn();

        String response = result.getResponse().getContentAsString();
        com.fasterxml.jackson.databind.JsonNode jsonNode = objectMapper.readTree(response);
        Long songId = jsonNode.get("data").get("id").asLong();
        String fileName = jsonNode.get("data").get("fileName").asText();

        mockOSSManager.simulateUpload(fileName);

        java.util.HashMap<String, Object> batchCompleteDto = new java.util.HashMap<>();
        java.util.List<Long> songIds = new java.util.ArrayList<>();
        songIds.add(songId);
        batchCompleteDto.put("songIds", songIds);
        String completeJson = objectMapper.writeValueAsString(batchCompleteDto);

        mockMvc.perform(post("/api/songs/batch-complete")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(completeJson)
                        .cookie(new org.springframework.mock.web.MockCookie("satoken", cookie)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.successCount").value(1));
    }

    @Test
    void shouldBatchCompleteUploadWithoutLogin() throws Exception {
        java.util.HashMap<String, Object> batchCompleteDto = new java.util.HashMap<>();
        java.util.List<Long> songIds = new java.util.ArrayList<>();
        songIds.add(1L);
        batchCompleteDto.put("songIds", songIds);
        String completeJson = objectMapper.writeValueAsString(batchCompleteDto);

        mockMvc.perform(post("/api/songs/batch-complete")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(completeJson))
                .andExpect(status().is(401))
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void shouldBatchDeleteSuccessfully() throws Exception {
        User user = testDataHelper.createTestUser();
        Song song1 = testDataHelper.createTestSong(user.getId());
        Song song2 = testDataHelper.createTestSong(user.getId());

        String email = user.getEmail();
        String password = "test_password_123";

        java.util.HashMap<String, String> loginDto = new java.util.HashMap<>();
        loginDto.put("email", email);
        loginDto.put("password", password);
        String loginJson = objectMapper.writeValueAsString(loginDto);

        MvcResult result = mockMvc.perform(post("/api/sessions")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(loginJson))
                .andExpect(status().isOk())
                .andReturn();

        String cookie = result.getResponse().getCookie("satoken").getValue();

        java.util.HashMap<String, Object> batchDeleteDto = new java.util.HashMap<>();
        java.util.List<Long> songIds = new java.util.ArrayList<>();
        songIds.add(song1.getId());
        songIds.add(song2.getId());
        batchDeleteDto.put("songIds", songIds);
        String deleteJson = objectMapper.writeValueAsString(batchDeleteDto);

        mockMvc.perform(post("/api/songs/batch")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(deleteJson)
                        .cookie(new org.springframework.mock.web.MockCookie("satoken", cookie)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.successCount").value(2));
    }

    @Test
    void shouldBatchDeleteWithoutLogin() throws Exception {
        java.util.HashMap<String, Object> batchDeleteDto = new java.util.HashMap<>();
        java.util.List<Long> songIds = new java.util.ArrayList<>();
        songIds.add(1L);
        batchDeleteDto.put("songIds", songIds);
        String deleteJson = objectMapper.writeValueAsString(batchDeleteDto);

        mockMvc.perform(post("/api/songs/batch")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(deleteJson))
                .andExpect(status().is(401))
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void shouldReturnErrorWhenBatchDeleteOthersSongs() throws Exception {
        Song song1 = testDataHelper.createTestSong(testUser.getId());

        User anotherUser = testDataHelper.createTestUser();
        String email = anotherUser.getEmail();
        String password = "test_password_123";

        java.util.HashMap<String, String> loginDto = new java.util.HashMap<>();
        loginDto.put("email", email);
        loginDto.put("password", password);
        String loginJson = objectMapper.writeValueAsString(loginDto);

        MvcResult result = mockMvc.perform(post("/api/sessions")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(loginJson))
                .andExpect(status().isOk())
                .andReturn();

        String cookie = result.getResponse().getCookie("satoken").getValue();

        java.util.HashMap<String, Object> batchDeleteDto = new java.util.HashMap<>();
        java.util.List<Long> songIds = new java.util.ArrayList<>();
        songIds.add(song1.getId());
        batchDeleteDto.put("songIds", songIds);
        String deleteJson = objectMapper.writeValueAsString(batchDeleteDto);

        mockMvc.perform(post("/api/songs/batch")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(deleteJson)
                        .cookie(new org.springframework.mock.web.MockCookie("satoken", cookie)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.failureCount").value(1));
    }

    @Test
    void shouldCancelUploadSuccessfully() throws Exception {
        Song uploadingSong = testDataHelper.createTestUploadingSong(testUser.getId());

        String email = testUser.getEmail();
        String password = "test_password_123";

        java.util.HashMap<String, String> loginDto = new java.util.HashMap<>();
        loginDto.put("email", email);
        loginDto.put("password", password);
        String loginJson = objectMapper.writeValueAsString(loginDto);

        MvcResult result = mockMvc.perform(post("/api/sessions")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(loginJson))
                .andExpect(status().isOk())
                .andReturn();

        String cookie = result.getResponse().getCookie("satoken").getValue();

        mockMvc.perform(post("/api/songs/{songId}/cancel", uploadingSong.getId())
                        .cookie(new org.springframework.mock.web.MockCookie("satoken", cookie)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("取消上传成功"));
    }

    @Test
    void shouldReturnErrorWhenCancelWithoutLogin() throws Exception {
        Song uploadingSong = testDataHelper.createTestUploadingSong(testUser.getId());

        mockMvc.perform(post("/api/songs/{songId}/cancel", uploadingSong.getId()))
                .andExpect(status().is(401))
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void shouldReturnErrorWhenCancelNonUploadingSong() throws Exception {
        String email = testUser.getEmail();
        String password = "test_password_123";

        java.util.HashMap<String, String> loginDto = new java.util.HashMap<>();
        loginDto.put("email", email);
        loginDto.put("password", password);
        String loginJson = objectMapper.writeValueAsString(loginDto);

        MvcResult result = mockMvc.perform(post("/api/sessions")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(loginJson))
                .andExpect(status().isOk())
                .andReturn();

        String cookie = result.getResponse().getCookie("satoken").getValue();

        mockMvc.perform(post("/api/songs/{songId}/cancel", testSong.getId())
                        .cookie(new org.springframework.mock.web.MockCookie("satoken", cookie)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void shouldBatchCancelUploadSuccessfully() throws Exception {
        Song uploadingSong1 = testDataHelper.createTestUploadingSong(testUser.getId());
        Song uploadingSong2 = testDataHelper.createTestUploadingSong(testUser.getId());

        String email = testUser.getEmail();
        String password = "test_password_123";

        java.util.HashMap<String, String> loginDto = new java.util.HashMap<>();
        loginDto.put("email", email);
        loginDto.put("password", password);
        String loginJson = objectMapper.writeValueAsString(loginDto);

        MvcResult result = mockMvc.perform(post("/api/sessions")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(loginJson))
                .andExpect(status().isOk())
                .andReturn();

        String cookie = result.getResponse().getCookie("satoken").getValue();

        java.util.HashMap<String, Object> batchCancelDto = new java.util.HashMap<>();
        java.util.List<Long> songIds = new java.util.ArrayList<>();
        songIds.add(uploadingSong1.getId());
        songIds.add(uploadingSong2.getId());
        batchCancelDto.put("songIds", songIds);
        String cancelJson = objectMapper.writeValueAsString(batchCancelDto);

        mockMvc.perform(post("/api/songs/batch-cancel")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(cancelJson)
                        .cookie(new org.springframework.mock.web.MockCookie("satoken", cookie)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.successCount").value(2));
    }

    @Test
    void shouldReturnErrorWhenDeleteNonNormalSong() throws Exception {
        Song uploadingSong = testDataHelper.createTestUploadingSong(testUser.getId());

        String email = testUser.getEmail();
        String password = "test_password_123";

        java.util.HashMap<String, String> loginDto = new java.util.HashMap<>();
        loginDto.put("email", email);
        loginDto.put("password", password);
        String loginJson = objectMapper.writeValueAsString(loginDto);

        MvcResult result = mockMvc.perform(post("/api/sessions")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(loginJson))
                .andExpect(status().isOk())
                .andReturn();

        String cookie = result.getResponse().getCookie("satoken").getValue();

        mockMvc.perform(delete("/api/songs/{songId}", uploadingSong.getId())
                        .cookie(new org.springframework.mock.web.MockCookie("satoken", cookie)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void shouldReturnErrorWhenGetNonNormalSong() throws Exception {
        Song uploadingSong = testDataHelper.createTestUploadingSong(testUser.getId());

        mockMvc.perform(get("/api/songs/{songId}", uploadingSong.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void shouldReturnErrorWhenUpdateNonNormalSong() throws Exception {
        Song uploadingSong = testDataHelper.createTestUploadingSong(testUser.getId());

        String email = testUser.getEmail();
        String password = "test_password_123";

        java.util.HashMap<String, String> loginDto = new java.util.HashMap<>();
        loginDto.put("email", email);
        loginDto.put("password", password);
        String loginJson = objectMapper.writeValueAsString(loginDto);

        MvcResult result = mockMvc.perform(post("/api/sessions")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(loginJson))
                .andExpect(status().isOk())
                .andReturn();

        String cookie = result.getResponse().getCookie("satoken").getValue();

        java.util.HashMap<String, Object> updateDto = new java.util.HashMap<>();
        updateDto.put("name", "Updated Title");
        String updateJson = objectMapper.writeValueAsString(updateDto);

        mockMvc.perform(patch("/api/songs/{songId}", uploadingSong.getId())
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(updateJson)
                        .cookie(new org.springframework.mock.web.MockCookie("satoken", cookie)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false));
    }
}
