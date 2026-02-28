# 批量歌曲操作功能实现计划

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 实现批量上传歌曲和批量删除歌曲功能，支持部分成功返回详情。

**Architecture:** 沿用现有两步上传流程（prepare + complete），新增批量接口。Controller 调用 Service 层新增的批量方法，Service 层复用现有单首歌曲处理逻辑并收集结果。

**Tech Stack:** Spring Boot 3.5, Jakarta Validation, MyBatis-Plus

---

## Task 1: 创建响应 DTO 类

**Files:**
- Create: `src/main/java/top/enderliquid/audioflow/dto/response/BatchFailureItem.java`
- Create: `src/main/java/top/enderliquid/audioflow/dto/response/SongBatchResultVO.java`

**Step 1: 创建 BatchFailureItem 类**

```java
package top.enderliquid.audioflow.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BatchFailureItem {
    private Integer index;
    private Long id;
    private String reason;
}
```

**Step 2: 创建 SongBatchResultVO 泛型类**

```java
package top.enderliquid.audioflow.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SongBatchResultVO<T> {
    private List<T> successList = new ArrayList<>();
    private List<BatchFailureItem> failureList = new ArrayList<>();
    private int successCount;
    private int failureCount;
}
```

**Step 3: 提交**

```bash
git add src/main/java/top/enderliquid/audioflow/dto/response/BatchFailureItem.java src/main/java/top/enderliquid/audioflow/dto/response/SongBatchResultVO.java
git commit -m "添加批量操作响应DTO类"
```

---

## Task 2: 创建请求 DTO 类

**Files:**
- Create: `src/main/java/top/enderliquid/audioflow/dto/request/song/SongBatchPrepareDTO.java`
- Create: `src/main/java/top/enderliquid/audioflow/dto/request/song/SongBatchCompleteDTO.java`
- Create: `src/main/java/top/enderliquid/audioflow/dto/request/song/SongBatchDeleteDTO.java`

**Step 1: 创建 SongBatchPrepareDTO 类**

```java
package top.enderliquid.audioflow.dto.request.song;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SongBatchPrepareDTO {
    @Valid
    @Size(min = 1, max = 10, message = "批量上传数量必须在1-10之间")
    private List<SongPrepareUploadDTO> songs;
}
```

**Step 2: 创建 SongBatchCompleteDTO 类**

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
    @Size(max = 10, message = "批量确认数量不能超过10")
    private List<Long> songIds;
}
```

**Step 3: 创建 SongBatchDeleteDTO 类**

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
    @Size(max = 10, message = "批量删除数量不能超过10")
    private List<Long> songIds;
}
```

**Step 4: 提交**

```bash
git add src/main/java/top/enderliquid/audioflow/dto/request/song/SongBatchPrepareDTO.java src/main/java/top/enderliquid/audioflow/dto/request/song/SongBatchCompleteDTO.java src/main/java/top/enderliquid/audioflow/dto/request/song/SongBatchDeleteDTO.java
git commit -m "添加批量操作请求DTO类"
```

---

## Task 3: 扩展 Service 接口

**Files:**
- Modify: `src/main/java/top/enderliquid/audioflow/service/SongService.java`

**Step 1: 添加批量方法声明**

在 `SongService.java` 中添加导入和方法声明：

```java
// 添加导入
import top.enderliquid.audioflow.dto.request.song.SongBatchCompleteDTO;
import top.enderliquid.audioflow.dto.request.song.SongBatchDeleteDTO;
import top.enderliquid.audioflow.dto.request.song.SongBatchPrepareDTO;
import top.enderliquid.audioflow.dto.response.SongBatchResultVO;

// 在接口末尾添加方法声明
SongBatchResultVO<SongUploadPrepareVO> batchPrepareUpload(@Valid SongBatchPrepareDTO dto, @NotNull(message = "用户Id不能为空") Long userId);

SongBatchResultVO<SongVO> batchCompleteUpload(@Valid SongBatchCompleteDTO dto, @NotNull(message = "用户Id不能为空") Long userId);

SongBatchResultVO<Long> batchRemoveSongs(@Valid SongBatchDeleteDTO dto, @NotNull(message = "用户Id不能为空") Long userId);
```

**Step 2: 提交**

```bash
git add src/main/java/top/enderliquid/audioflow/service/SongService.java
git commit -m "添加批量操作方法声明到Service接口"
```

---

## Task 4: 实现批量准备上传方法

**Files:**
- Modify: `src/main/java/top/enderliquid/audioflow/service/impl/SongServiceImpl.java`

**Step 1: 添加导入和方法实现**

在 `SongServiceImpl.java` 中添加：

```java
// 添加导入
import top.enderliquid.audioflow.dto.request.song.SongBatchCompleteDTO;
import top.enderliquid.audioflow.dto.request.song.SongBatchDeleteDTO;
import top.enderliquid.audioflow.dto.request.song.SongBatchPrepareDTO;
import top.enderliquid.audioflow.dto.response.BatchFailureItem;
import top.enderliquid.audioflow.dto.response.SongBatchResultVO;

// 在类末尾添加方法实现
@Override
public SongBatchResultVO<SongUploadPrepareVO> batchPrepareUpload(SongBatchPrepareDTO dto, Long userId) {
    log.info("请求批量准备上传歌曲，用户ID: {}, 数量: {}", userId, dto.getSongs().size());
    SongBatchResultVO<SongUploadPrepareVO> result = new SongBatchResultVO<>();
    List<SongPrepareUploadDTO> songs = dto.getSongs();
    for (int i = 0; i < songs.size(); i++) {
        SongPrepareUploadDTO songDto = songs.get(i);
        try {
            SongUploadPrepareVO prepareVO = prepareUpload(songDto, userId);
            result.getSuccessList().add(prepareVO);
        } catch (BusinessException e) {
            BatchFailureItem failureItem = new BatchFailureItem(i, null, e.getMessage());
            result.getFailureList().add(failureItem);
        }
    }
    result.setSuccessCount(result.getSuccessList().size());
    result.setFailureCount(result.getFailureList().size());
    log.info("批量准备上传歌曲完成，成功: {}, 失败: {}", result.getSuccessCount(), result.getFailureCount());
    return result;
}
```

**Step 2: 提交**

```bash
git add src/main/java/top/enderliquid/audioflow/service/impl/SongServiceImpl.java
git commit -m "实现批量准备上传方法"
```

---

## Task 5: 实现批量确认上传方法

**Files:**
- Modify: `src/main/java/top/enderliquid/audioflow/service/impl/SongServiceImpl.java`

**Step 1: 在 SongServiceImpl 类中添加方法实现**

```java
@Override
public SongBatchResultVO<SongVO> batchCompleteUpload(SongBatchCompleteDTO dto, Long userId) {
    log.info("请求批量确认上传歌曲，用户ID: {}, 数量: {}", userId, dto.getSongIds().size());
    SongBatchResultVO<SongVO> result = new SongBatchResultVO<>();
    List<Long> songIds = dto.getSongIds();
    for (int i = 0; i < songIds.size(); i++) {
        Long songId = songIds.get(i);
        try {
            SongVO songVO = completeUpload(new SongCompleteUploadDTO(songId), userId);
            result.getSuccessList().add(songVO);
        } catch (BusinessException e) {
            BatchFailureItem failureItem = new BatchFailureItem(i, songId, e.getMessage());
            result.getFailureList().add(failureItem);
        }
    }
    result.setSuccessCount(result.getSuccessList().size());
    result.setFailureCount(result.getFailureList().size());
    log.info("批量确认上传歌曲完成，成功: {}, 失败: {}", result.getSuccessCount(), result.getFailureCount());
    return result;
}
```

**Step 2: 提交**

```bash
git add src/main/java/top/enderliquid/audioflow/service/impl/SongServiceImpl.java
git commit -m "实现批量确认上传方法"
```

---

## Task 6: 实现批量删除方法

**Files:**
- Modify: `src/main/java/top/enderliquid/audioflow/service/impl/SongServiceImpl.java`

**Step 1: 在 SongServiceImpl 类中添加方法实现**

```java
@Override
public SongBatchResultVO<Long> batchRemoveSongs(SongBatchDeleteDTO dto, Long userId) {
    log.info("请求批量删除歌曲，用户ID: {}, 数量: {}", userId, dto.getSongIds().size());
    SongBatchResultVO<Long> result = new SongBatchResultVO<>();
    List<Long> songIds = dto.getSongIds();
    for (int i = 0; i < songIds.size(); i++) {
        Long songId = songIds.get(i);
        try {
            removeSong(songId, userId);
            result.getSuccessList().add(songId);
        } catch (BusinessException e) {
            BatchFailureItem failureItem = new BatchFailureItem(i, songId, e.getMessage());
            result.getFailureList().add(failureItem);
        }
    }
    result.setSuccessCount(result.getSuccessList().size());
    result.setFailureCount(result.getFailureList().size());
    log.info("批量删除歌曲完成，成功: {}, 失败: {}", result.getSuccessCount(), result.getFailureCount());
    return result;
}
```

**Step 2: 提交**

```bash
git add src/main/java/top/enderliquid/audioflow/service/impl/SongServiceImpl.java
git commit -m "实现批量删除方法"
```

---

## Task 7: 添加 Controller 批量接口

**Files:**
- Modify: `src/main/java/top/enderliquid/audioflow/controller/SongController.java`

**Step 1: 添加导入**

```java
import top.enderliquid.audioflow.dto.request.song.SongBatchCompleteDTO;
import top.enderliquid.audioflow.dto.request.song.SongBatchDeleteDTO;
import top.enderliquid.audioflow.dto.request.song.SongBatchPrepareDTO;
import top.enderliquid.audioflow.dto.response.BatchFailureItem;
import top.enderliquid.audioflow.dto.response.SongBatchResultVO;
import top.enderliquid.audioflow.dto.response.SongUploadPrepareVO;
```

**Step 2: 添加批量准备上传接口**

在 `SongController` 类中添加：

```java
/**
 * 批量准备上传歌曲
 * 需要登录
 */
@SaCheckLogin
@PostMapping("/batch-prepare")
@RateLimit(
        refillRate = "1/10",
        capacity = 3,
        limitType = LimitType.BOTH,
        message = "批量上传过于频繁，请稍后再试"
)
public HttpResponseBody<SongBatchResultVO<SongUploadPrepareVO>> batchPrepareUpload(@Valid @RequestBody SongBatchPrepareDTO dto) {
    long userId = StpUtil.getLoginIdAsLong();
    SongBatchResultVO<SongUploadPrepareVO> result = songService.batchPrepareUpload(dto, userId);
    return HttpResponseBody.ok(result);
}
```

**Step 3: 添加批量确认上传接口**

```java
/**
 * 批量完成上传歌曲
 * 需要登录
 */
@SaCheckLogin
@PostMapping("/batch-complete")
public HttpResponseBody<SongBatchResultVO<SongVO>> batchCompleteUpload(@Valid @RequestBody SongBatchCompleteDTO dto) {
    long userId = StpUtil.getLoginIdAsLong();
    SongBatchResultVO<SongVO> result = songService.batchCompleteUpload(dto, userId);
    return HttpResponseBody.ok(result);
}
```

**Step 4: 添加批量删除接口**

```java
/**
 * 批量删除歌曲
 * 需要登录
 */
@SaCheckLogin
@PostMapping("/batch")
@RateLimit(
        refillRate = "1/10",
        capacity = 3,
        limitType = LimitType.BOTH,
        message = "批量删除过于频繁，请稍后再试"
)
public HttpResponseBody<SongBatchResultVO<Long>> batchRemoveSongs(@Valid @RequestBody SongBatchDeleteDTO dto) {
    long userId = StpUtil.getLoginIdAsLong();
    SongBatchResultVO<Long> result = songService.batchRemoveSongs(dto, userId);
    return HttpResponseBody.ok(result);
}
```

**Step 5: 提交**

```bash
git add src/main/java/top/enderliquid/audioflow/controller/SongController.java
git commit -m "添加批量操作Controller接口"
```

---

## Task 8: 编写单元测试

**Files:**
- Create: `src/test/java/top/enderliquid/audioflow/service/SongBatchServiceTest.java`

**Step 1: 创建测试类**

```java
package top.enderliquid.audioflow.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import top.enderliquid.audioflow.common.exception.BusinessException;
import top.enderliquid.audioflow.dto.request.song.SongBatchCompleteDTO;
import top.enderliquid.audioflow.dto.request.song.SongBatchDeleteDTO;
import top.enderliquid.audioflow.dto.request.song.SongBatchPrepareDTO;
import top.enderliquid.audioflow.dto.request.song.SongPrepareUploadDTO;
import top.enderliquid.audioflow.dto.response.SongBatchResultVO;
import top.enderliquid.audioflow.dto.response.SongUploadPrepareVO;
import top.enderliquid.audioflow.dto.response.SongVO;
import top.enderliquid.audioflow.entity.Song;
import top.enderliquid.audioflow.entity.User;
import top.enderliquid.audioflow.manager.OSSManager;
import top.enderliquid.audioflow.manager.SongManager;
import top.enderliquid.audioflow.manager.UserManager;
import top.enderliquid.audioflow.service.impl.SongServiceImpl;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SongBatchServiceTest {

    @Mock
    private UserManager userManager;

    @Mock
    private SongManager songManager;

    @Mock
    private OSSManager ossManager;

    @InjectMocks
    private SongServiceImpl songService;

    private User testUser;
    private Long testUserId = 1L;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(testUserId);
        testUser.setName("测试用户");
    }

    @Test
    @DisplayName("批量准备上传-成功")
    void batchPrepareUpload_success() {
        when(userManager.getById(testUserId)).thenReturn(testUser);
        when(songManager.save(any(Song.class))).thenReturn(true);
        when(ossManager.generatePresignedPutUrl(anyString(), anyString())).thenReturn("https://test.url");

        List<SongPrepareUploadDTO> songs = new ArrayList<>();
        songs.add(new SongPrepareUploadDTO("audio/mpeg", "歌曲1", "描述1"));
        songs.add(new SongPrepareUploadDTO("audio/mpeg", "歌曲2", "描述2"));

        SongBatchPrepareDTO dto = new SongBatchPrepareDTO(songs);

        SongBatchResultVO<SongUploadPrepareVO> result = songService.batchPrepareUpload(dto, testUserId);

        assertEquals(2, result.getSuccessCount());
        assertEquals(0, result.getFailureCount());
    }

    @Test
    @DisplayName("批量准备上传-部分失败")
    void batchPrepareUpload_partialFailure() {
        when(userManager.getById(testUserId)).thenReturn(testUser);
        when(songManager.save(any(Song.class))).thenReturn(true);
        when(ossManager.generatePresignedPutUrl(anyString(), anyString())).thenReturn("https://test.url");

        List<SongPrepareUploadDTO> songs = new ArrayList<>();
        songs.add(new SongPrepareUploadDTO("audio/mpeg", "歌曲1", "描述1"));
        songs.add(new SongPrepareUploadDTO("invalid/type", "歌曲2", "描述2"));

        SongBatchPrepareDTO dto = new SongBatchPrepareDTO(songs);

        SongBatchResultVO<SongUploadPrepareVO> result = songService.batchPrepareUpload(dto, testUserId);

        assertEquals(1, result.getSuccessCount());
        assertEquals(1, result.getFailureCount());
        assertEquals("不支持该文件类型", result.getFailureList().get(0).getReason());
    }

    @Test
    @DisplayName("批量删除-部分成功")
    void batchRemoveSongs_partialSuccess() {
        Song song1 = new Song();
        song1.setId(1L);
        song1.setUploaderId(testUserId);
        song1.setFileName("1.mp3");

        Song song2 = new Song();
        song2.setId(2L);
        song2.setUploaderId(999L);
        song2.setFileName("2.mp3");

        when(userManager.getById(testUserId)).thenReturn(testUser);
        when(songManager.getById(1L)).thenReturn(song1);
        when(songManager.getById(2L)).thenReturn(song2);
        when(songManager.removeById(any(Song.class))).thenReturn(true);
        when(ossManager.deleteFile(anyString())).thenReturn(true);

        List<Long> songIds = List.of(1L, 2L);
        SongBatchDeleteDTO dto = new SongBatchDeleteDTO(songIds);

        SongBatchResultVO<Long> result = songService.batchRemoveSongs(dto, testUserId);

        assertEquals(1, result.getSuccessCount());
        assertEquals(1, result.getFailureCount());
    }
}
```

**Step 2: 运行测试**

```bash
./mvnw test -Dtest=SongBatchServiceTest
```

**Step 3: 提交**

```bash
git add src/test/java/top/enderliquid/audioflow/service/SongBatchServiceTest.java
git commit -m "添加批量操作Service单元测试"
```

---

## Task 9: 编译验证

**Step 1: 编译项目**

```bash
./mvnw clean compile
```

**Step 2: 运行所有测试**

```bash
./mvnw test
```

**Step 3: 提交（如有修改）**

如果有任何修改，在此提交。

---

## 总结

实现了以下功能：
1. `POST /api/songs/batch-prepare` - 批量准备上传（最多 10 首）
2. `POST /api/songs/batch-complete` - 批量确认上传
3. `POST /api/songs/batch` (X-HTTP-Method-Override: DELETE) - 批量删除

所有批量操作均返回 `SongBatchResultVO<T>` 格式，包含成功列表、失败列表及计数。