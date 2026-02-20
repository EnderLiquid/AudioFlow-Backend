package top.enderliquid.audioflow.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MvcResult;
import top.enderliquid.audioflow.common.TestDataHelper;
import top.enderliquid.audioflow.config.BaseControllerTest;
import top.enderliquid.audioflow.entity.Song;
import top.enderliquid.audioflow.entity.User;

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
}
