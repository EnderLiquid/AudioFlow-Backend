# 批量歌曲操作实现计划

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 实现批量上传和批量删除歌曲功能，复用现有单首歌曲操作逻辑

**Architecture:** 在 Controller 层添加三个新端点，Service 层添加批量处理方法，新增 DTO 和 VO 类

**Tech Stack:** Spring Boot 3.5, MyBatis-Plus, Jakarta Validation

---

## Task 1: 创建批量操作 DTO 类

**Files:**

- Create: `src/main/java/top/enderliquid/audioflow/dto/request/song/SongBatchPrepareDTO.java`
- Create: `src/main/java/top/enderliquid/audioflow/dto/request/song/SongBatchCompleteDTO.java`
- Create: `src/main/java/top/enderliquid/audioflow/dto/request/song/SongBatchDeleteDTO.java`

**Step 1: 创建 SongBatchPrepareDTO**

```java
package top.enderliquid.audioflow.dto.request.song;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SongBatchPrepareDTO {
    @NotEmpty(message = "歌曲列表不能为空")
    @Size(max = 10, message = "一次最多上传10首歌曲")
    @Valid
    private List<SongPrepareUploadDTO> songs;
}
```

**Step 2: 创建 SongBatchCompleteDTO**

```java
package top.enderliquid.audioflow.dto.request.song;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SongBatchCompleteDTO {
    @NotEmpty(message = "歌曲ID列表不能为空")
    @Size(max = 10, message = "一次最多确认10首歌曲")
    private List<Long> songIds;
}
```

**Step 3: 创建 SongBatchDeleteDTO**

```java
package top.enderliquid.audioflow.dto.request.song;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SongBatchDeleteDTO {
    @NotEmpty(message = "歌曲ID列表不能为空")
    @Size(max = 50, message = "一次最多删除50首歌曲")
    private List<Long> songIds;
}
```

**Step 4: 提交**

```bash
git add src/main/java/top/enderliquid/audioflow/dto/request/song/SongBatch*.java
git commit -m "添加批量歌曲操作 DTO 类"
```

---

## Task 2: 创建批量操作 VO 类

**Files:**

- Create: `src/main/java/top/enderliquid/audioflow/dto/response/SongBatchPrepareVO.java`
- Create: `src/main/java/top/enderliquid/audioflow/dto/response/BatchFailureItem.java`
- Create: `src/main/java/top/enderliquid/audioflow/dto/response/SongBatchResultVO.java`

**Step 1: 创建 SongBatchPrepareVO**

```java
package top.enderliquid.audioflow.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import top.enderliquid.audioflow.dto.response.song.SongUploadPrepareVO;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SongBatchPrepareVO {
    private List<SongUploadPrepareVO> preparedSongs;
}
```

**Step 2: 创建 BatchFailureItem**

```java
package top.enderliquid.audioflow.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BatchFailureItem {
    private Long id;
    private String reason;
}
```

**Step 3: 创建 SongBatchResultVO**

```java
package top.enderliquid.audioflow.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SongBatchResultVO<T> {
    private List<T> successList;
    private List<BatchFailureItem> failureList;
    private Integer successCount;
    private Integer failureCount;
}
```

**Step 4: 提交**

```bash
git add src/main/java/top/enderliquid/audioflow/dto/response/SongBatchPrepareVO.java src/main/java/top/enderliquid/audioflow/dto/response/BatchFailureItem.java src/main/java/top/enderliquid/audioflow/dto/response/SongBatchResultVO.java
git commit -m "添加批量歌曲操作 VO 类"
```

---

## Task 3: 扩展 SongService 接口

**Files:**

- Modify: `src/main/java/top/enderliquid/audioflow/service/SongService.java`

**Step 1: 添加批量操作方法签名**

在 `SongService.java` 接口中添加以下方法：

```java
SongBatchPrepareVO prepareBatchUpload(@Valid SongBatchPrepareDTO dto, @NotNull(message = "用户Id不能为空") Long userId);

SongBatchResultVO<SongVO> completeBatchUpload(@Valid SongBatchCompleteDTO dto, @NotNull(message = "用户Id不能为空") Long userId);

SongBatchResultVO<Long> removeBatchSongs(@Valid SongBatchDeleteDTO dto, @NotNull(message = "用户Id不能为空") Long userId);
```

添加必要的 import：

```java
import top.enderliquid.audioflow.dto.request.song.SongBatchCompleteDTO;
import top.enderliquid.audioflow.dto.request.song.SongBatchDeleteDTO;
import top.enderliquid.audioflow.dto.request.song.SongBatchPrepareDTO;
import top.enderliquid.audioflow.dto.response.SongBatchPrepareVO;

```

**Step 2: 提交**

```bash
git add src/main/java/top/enderliquid/audioflow/service/SongService.java
git commit -m "添加批量歌曲操作 Service 接口方法"
```

---

## Task 4: 实现 SongServiceImpl 批量操作方法

**Files:**

- Modify: `src/main/java/top/enderliquid/audioflow/service/impl/SongServiceImpl.java`

**Step 1: 添加 import**

```java
import top.enderliquid.audioflow.dto.request.song.SongBatchCompleteDTO;
import top.enderliquid.audioflow.dto.request.song.SongBatchDeleteDTO;
import top.enderliquid.audioflow.dto.request.song.SongBatchPrepareDTO;
import top.enderliquid.audioflow.dto.response.SongBatchPrepareVO;

```

**Step 2: 实现 prepareBatchUpload 方法**

```java
@Override
public SongBatchPrepareVO prepareBatchUpload(SongBatchPrepareDTO dto, Long userId) {
    log.info("请求批量准备上传歌曲，用户ID: {}，数量: {}", userId, dto.getSongs().size());
    List<SongUploadPrepareVO> preparedSongs = new ArrayList<>();
    for (SongPrepareUploadDTO songDto : dto.getSongs()) {
        try {
            SongUploadPrepareVO prepareVO = prepareUpload(songDto, userId);
            preparedSongs.add(prepareVO);
        } catch (BusinessException e) {
            log.warn("批量准备上传歌曲失败，歌曲名称: {}，原因: {}", songDto.getName(), e.getMessage());
        }
    }
    log.info("批量准备上传歌曲成功，成功数量: {}", preparedSongs.size());
    SongBatchPrepareVO result = new SongBatchPrepareVO();
    result.setPreparedSongs(preparedSongs);
    return result;
}
```

**Step 3: 实现 completeBatchUpload 方法**

```java
@Override
public SongBatchResultVO<SongVO> completeBatchUpload(SongBatchCompleteDTO dto, Long userId) {
    log.info("请求批量确认上传歌曲，用户ID: {}，数量: {}", userId, dto.getSongIds().size());
    List<SongVO> successList = new ArrayList<>();
    List<BatchFailureItem> failureList = new ArrayList<>();
    for (Long songId : dto.getSongIds()) {
        try {
            SongCompleteUploadDTO completeDTO = new SongCompleteUploadDTO();
            completeDTO.setSongId(songId);
            SongVO songVO = completeUpload(completeDTO, userId);
            successList.add(songVO);
        } catch (BusinessException e) {
            log.warn("批量确认上传歌曲失败，歌曲ID: {}，原因: {}", songId, e.getMessage());
            failureList.add(new BatchFailureItem(songId, e.getMessage()));
        }
    }
    log.info("批量确认上传歌曲完成，成功: {}，失败: {}", successList.size(), failureList.size());
    SongBatchResultVO<SongVO> result = new SongBatchResultVO<>();
    result.setSuccessList(successList);
    result.setFailureList(failureList);
    result.setSuccessCount(successList.size());
    result.setFailureCount(failureList.size());
    return result;
}
```

**Step 4: 实现 removeBatchSongs 方法**

```java
@Override
public SongBatchResultVO<Long> removeBatchSongs(SongBatchDeleteDTO dto, Long userId) {
    log.info("请求批量删除歌曲，用户ID: {}，数量: {}", userId, dto.getSongIds().size());
    List<Long> successList = new ArrayList<>();
    List<BatchFailureItem> failureList = new ArrayList<>();
    for (Long songId : dto.getSongIds()) {
        try {
            removeSong(songId, userId);
            successList.add(songId);
        } catch (BusinessException e) {
            log.warn("批量删除歌曲失败，歌曲ID: {}，原因: {}", songId, e.getMessage());
            failureList.add(new BatchFailureItem(songId, e.getMessage()));
        }
    }
    log.info("批量删除歌曲完成，成功: {}，失败: {}", successList.size(), failureList.size());
    SongBatchResultVO<Long> result = new SongBatchResultVO<>();
    result.setSuccessList(successList);
    result.setFailureList(failureList);
    result.setSuccessCount(successList.size());
    result.setFailureCount(failureList.size());
    return result;
}
```

**Step 5: 提交**

```bash
git add src/main/java/top/enderliquid/audioflow/service/impl/SongServiceImpl.java
git commit -m "实现批量歌曲操作 Service 方法"
```

---

## Task 5: 添加 Controller 端点

**Files:**

- Modify: `src/main/java/top/enderliquid/audioflow/controller/SongController.java`

**Step 1: 添加 import**

```java
import top.enderliquid.audioflow.dto.request.song.SongBatchCompleteDTO;
import top.enderliquid.audioflow.dto.request.song.SongBatchDeleteDTO;
import top.enderliquid.audioflow.dto.request.song.SongBatchPrepareDTO;
import top.enderliquid.audioflow.dto.response.SongBatchPrepareVO;

```

**Step 2: 添加批量准备上传端点**

```java
@SaCheckLogin
@PostMapping("/batch-prepare")
@RateLimit(
        refillRate = "1/5",
        capacity = 3,
        limitType = LimitType.BOTH,
        message = "批量上传请求过于频繁，请稍后再试"
)
public HttpResponseBody<SongBatchPrepareVO> prepareBatchUpload(@Valid @RequestBody SongBatchPrepareDTO dto) {
    long userId = StpUtil.getLoginIdAsLong();
    SongBatchPrepareVO prepareVO = songService.prepareBatchUpload(dto, userId);
    return HttpResponseBody.ok(prepareVO);
}
```

**Step 3: 添加批量确认上传端点**

```java
@SaCheckLogin
@PostMapping("/batch-complete")
@RateLimit(
        refillRate = "1/3",
        capacity = 5,
        limitType = LimitType.BOTH,
        message = "批量确认请求过于频繁，请稍后再试"
)
public HttpResponseBody<SongBatchResultVO<SongVO>> completeBatchUpload(@Valid @RequestBody SongBatchCompleteDTO dto) {
    long userId = StpUtil.getLoginIdAsLong();
    SongBatchResultVO<SongVO> result = songService.completeBatchUpload(dto, userId);
    return HttpResponseBody.ok(result);
}
```

**Step 4: 添加批量删除端点**

```java
@SaCheckLogin
@PostMapping("/batch")
@RateLimit(
        refillRate = "1/3",
        capacity = 5,
        limitType = LimitType.BOTH,
        message = "批量删除请求过于频繁，请稍后再试"
)
public HttpResponseBody<SongBatchResultVO<Long>> removeBatchSongs(@Valid @RequestBody SongBatchDeleteDTO dto) {
    long userId = StpUtil.getLoginIdAsLong();
    SongBatchResultVO<Long> result = songService.removeBatchSongs(dto, userId);
    return HttpResponseBody.ok(result);
}
```

**Step 5: 提交**

```bash
git add src/main/java/top/enderliquid/audioflow/controller/SongController.java
git commit -m "添加批量歌曲操作 Controller 端点"
```

---

## Task 6: 编译验证

**Step 1: 编译项目**

Run: `./mvnw clean compile`
Expected: BUILD SUCCESS

**Step 2: 提交（如有修复）**

如有编译错误需要修复，修复后提交：

```bash
git add -A
git commit -m "修复编译错误"
```

---

## Task 7: 编写单元测试

**Files:**

- Create: `src/test/java/top/enderliquid/audioflow/controller/SongBatchControllerTest.java`

**Step 1: 创建测试类框架**

```java
package top.enderliquid.audioflow.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SongBatchControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String userToken;
    private String adminToken;

    @BeforeEach
    void setUp() throws Exception {
        // 登录获取token
        userToken = loginAndGetToken("user1", "password1");
        adminToken = loginAndGetToken("admin", "admin123");
    }

    private String loginAndGetToken(String username, String password) throws Exception {
        String response = mockMvc.perform(post("/api/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"))
                .andReturn().getResponse().getContentAsString();
        // 从响应中提取token
        return objectMapper.readTree(response).path("data").path("token").asText();
    }

    @Test
    @DisplayName("批量准备上传 - 成功")
    void shouldPrepareBatchUpload() throws Exception {
        String body = "{\"songs\":[" +
                "{\"mimeType\":\"audio/mpeg\",\"name\":\"歌曲1\",\"description\":\"描述1\"}," +
                "{\"mimeType\":\"audio/flac\",\"name\":\"歌曲2\",\"description\":\"描述2\"}" +
                "]}";

        mockMvc.perform(post("/api/songs/batch-prepare")
                        .header("Authorization", userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.preparedSongs").isArray())
                .andExpect(jsonPath("$.data.preparedSongs.length()").value(2));
    }

    @Test
    @DisplayName("批量准备上传 - 超过上限应失败")
    void shouldFailWhenBatchPrepareExceedsLimit() throws Exception {
        StringBuilder songs = new StringBuilder("{\"songs\":[");
        for (int i = 0; i < 11; i++) {
            if (i > 0) songs.append(",");
            songs.append("{\"mimeType\":\"audio/mpeg\",\"name\":\"歌曲").append(i).append("\"}");
        }
        songs.append("]}");

        mockMvc.perform(post("/api/songs/batch-prepare")
                        .header("Authorization", userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(songs.toString()))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("批量准备上传 - 未登录应失败")
    void shouldFailBatchPrepareWithoutLogin() throws Exception {
        mockMvc.perform(post("/api/songs/batch-prepare")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"songs\":[{\"mimeType\":\"audio/mpeg\",\"name\":\"歌曲1\"}]}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("批量删除 - 未登录应失败")
    void shouldFailBatchDeleteWithoutLogin() throws Exception {
        mockMvc.perform(post("/api/songs/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"songIds\":[1,2,3]}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("批量删除 - 空列表应失败")
    void shouldFailBatchDeleteWithEmptyList() throws Exception {
        mockMvc.perform(post("/api/songs/batch")
                        .header("Authorization", userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"songIds\":[]}"))
                .andExpect(status().isBadRequest());
    }
}
```

**Step 2: 运行测试**

Run: `./mvnw test -Dtest=SongBatchControllerTest`
Expected: 部分测试通过

**Step 3: 提交**

```bash
git add src/test/java/top/enderliquid/audioflow/controller/SongBatchControllerTest.java
git commit -m "添加批量歌曲操作单元测试"
```

---

## Task 8: 运行全部测试并验证

**Step 1: 运行所有测试**

Run: `./mvnw test`
Expected: BUILD SUCCESS，所有测试通过

**Step 2: 最终提交**

```bash
git add -A
git commit -m "完成批量歌曲操作功能"
```