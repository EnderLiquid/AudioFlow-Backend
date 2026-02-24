# 歌曲上传流程重构实施计划

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 将歌曲上传从前端->后端->OSS的流程，改为前端先获取OSS临时上传URL，然后直传OSS的三步流程。取消策略模式的FileManager，改用专门的OSSManager。

**Architecture:** 删除FileManager策略模式，创建OSSManager直接管理S3操作。新增歌曲状态字段，实现三步上传流程（准备上传->直传OSS->完成验证），并添加定时清理超时上传记录的任务。

**Tech Stack:** Java 21, Spring Boot 3.5.10, Sa-Token, MyBatis-Plus, AWS S3 SDK, Apache Tika, Spring @Scheduled

---

### Task 1: 修改数据库脚本

**Files:**
- Modify: 查找create.sql文件路径
- Test: 无

**Step 1: 查找create.sql文件**

运行: `find . -name "create.sql" -type f`
预期: 找到create.sql文件路径

**Step 2: 读取create.sql文件**

使用Read工具读取找到的create.sql文件

**Step 3: 修改song表结构**

找到song表的CREATE TABLE语句，修改为：
```sql
CREATE TABLE `song` (
  `id` bigint NOT NULL COMMENT '歌曲ID',
  `name` varchar(64) NOT NULL COMMENT '歌曲名称',
  `description` varchar(128) NOT NULL COMMENT '歌曲描述',
  `file_name` varchar(255) NOT NULL COMMENT '文件名',
  `size` bigint NOT NULL COMMENT '文件大小（字节）',
  `duration` bigint NOT NULL COMMENT '歌曲持续时长（毫秒）',
  `uploader_id` bigint NOT NULL COMMENT '上传者ID',
  `status` varchar(20) NOT NULL DEFAULT 'UPLOADING' COMMENT '歌曲状态：UPLOADING-上传中, NORMAL-正常',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_uploader_id` (`uploader_id`),
  KEY `idx_status_create_time` (`status`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='歌曲表';
```

注意：删除source_type字段，添加status字段，新增idx_status_create_time索引用于定时清理查询

**Step 4: 提交修改**

运行:
```bash
git add <create.sql路径>
git commit -m "修改song表结构：添加status字段，删除source_type字段，新增索引"
```

---

### Task 2: 创建SongStatus枚举

**Files:**
- Create: `src/main/java/top/enderliquid/audioflow/common/enums/SongStatus.java`
- Test: 无

**Step 1: 创建SongStatus枚举文件**

```java
package top.enderliquid.audioflow.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum SongStatus {
    UPLOADING,
    NORMAL
}
```

**Step 2: 提交修改**

运行:
```bash
git add src/main/java/top/enderliquid/audioflow/common/enums/SongStatus.java
git commit -m "添加SongStatus枚举"
```

---

### Task 3: 创建SongPrepareUploadDTO

**Files:**
- Create: `src/main/java/top/enderliquid/audioflow/dto/request/song/SongPrepareUploadDTO.java`
- Test: 无

**Step 1: 创建SongPrepareUploadDTO文件**

```java
package top.enderliquid.audioflow.dto.request.song;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SongPrepareUploadDTO {
    @NotBlank(message = "文件类型不能为空")
    private String mimetype;

    @Size(min = 1, max = 64, message = "歌曲名称长度必须在1-64个字符之间")
    private String name;

    @Size(min = 1, max = 128, message = "描述长度必须在1-128个字符之间")
    private String description;
}
```

**Step 2: 提交修改**

运行:
```bash
git add src/main/java/top/enderliquid/audioflow/dto/request/song/SongPrepareUploadDTO.java
git commit -m "添加SongPrepareUploadDTO"
```

---

### Task 4: 创建SongCompleteUploadDTO

**Files:**
- Create: `src/main/java/top/enderliquid/audioflow/dto/request/song/SongCompleteUploadDTO.java`
- Test: 无

**Step 1: 创建SongCompleteUploadDTO文件**

```java
package top.enderliquid.audioflow.dto.request.song;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SongCompleteUploadDTO {
    @NotNull(message = "歌曲ID不能为空")
    private Long songId;
}
```

**Step 2: 提交修改**

运行:
```bash
git add src/main/java/top/enderliquid/audioflow/dto/request/song/SongCompleteUploadDTO.java
git commit -m "添加SongCompleteUploadDTO"
```

---

### Task 5: 创建SongUploadPrepareVO

**Files:**
- Create: `src/main/java/top/enderliquid/audioflow/dto/response/SongUploadPrepareVO.java`
- Test: 无

**Step 1: 创建SongUploadPrepareVO文件**

```java
package top.enderliquid.audioflow.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SongUploadPrepareVO {
    private Long id;
    private String fileName;
    private String uploadUrl;
}
```

**Step 2: 提交修改**

运行:
```bash
git add src/main/java/top/enderliquid/audioflow/dto/response/SongUploadPrepareVO.java
git commit -m "添加SongUploadPrepareVO"
```

---

### Task 6: 修改Song实体类

**Files:**
- Modify: `src/main/java/top/enderliquid/audioflow/entity/Song.java`
- Test: 无

**Step 1: 读取Song实体文件**

使用Read工具读取Song.java文件

**Step 2: 删除sourceType字段**

删除以下字段：
```java
@TableField("source_type")
private String sourceType;
```

**Step 3: 添加status字段**

在duration字段后添加：
```java
private String status;
```

**Step 4: 提交修改**

运行:
```bash
git add src/main/java/top/enderliquid/audioflow/entity/Song.java
git commit -m "修改Song实体：删除sourceType字段，添加status字段"
```

---

### Task 7: 修改SongBO

**Files:**
- Modify: `src/main/java/top/enderliquid/audioflow/dto/bo/SongBO.java`
- Test: 无

**Step 1: 读取SongBO文件**

使用Read工具读取SongBO.java文件

**Step 2: 删除sourceType字段**

删除以下字段：
```java
private String sourceType;
```

**Step 3: 添加status字段**

在uploaderId字段后添加：
```java
private String status;
```

**Step 4: 提交修改**

运行:
```bash
git add src/main/java/top/enderliquid/audioflow/dto/bo/SongBO.java
git commit -m "修改SongBO：删除sourceType字段，添加status字段"
```

---

### Task 8: 修改SongVO

**Files:**
- Modify: 查找SongVO.java文件路径
- Test: 无

**Step 1: 查找SongVO文件**

运行: `find . -name "SongVO.java" -type f`
预期: 找到SongVO.java文件路径

**Step 2: 读取SongVO文件**

使用Read工具读取SongVO.java文件

**Step 3: 删除sourceType字段**

删除sourceType字段

**Step 4: 添加status字段**

添加status字段

**Step 5: 提交修改**

运行:
```bash
git add <SongVO.java路径>
git commit -m "修改SongVO：删除sourceType字段，添加status字段"
```

---

### Task 9: 创建OSSManager接口

**Files:**
- Create: `src/main/java/top/enderliquid/audioflow/manager/OSSManager.java`
- Test: 无

**Step 1: 创建OSSManager接口文件**

```java
package top.enderliquid.audioflow.manager;

import org.springframework.lang.Nullable;

import java.io.InputStream;
import java.time.Duration;

public interface OSSManager {
    /**
     * 生成预签名POST上传URL（带Policy限制）
     *
     * @param fileName 文件名
     * @param expiration 过期时间
     * @return 预签名URL
     */
    @Nullable
    String generatePresignedPostUrl(String fileName, Duration expiration);

    /**
     * 检查文件是否存在
     *
     * @param fileName 文件名
     * @return 是否存在
     */
    boolean checkFileExists(String fileName);

    /**
     * 获取文件InputStream
     *
     * @param fileName 文件名
     * @return 文件流
     */
    @Nullable
    InputStream getFileInputStream(String fileName);

    /**
     * 删除文件
     *
     * @param fileName 文件名
     * @return 删除是否成功
     */
    boolean deleteFile(String fileName);

    /**
     * 获取预签名访问URL（播放用）
     *
     * @param fileName 文件名
     * @param expiration 过期时间
     * @return 预签名URL
     */
    @Nullable
    String getPresignedGetUrl(String fileName, Duration expiration);

    /**
     * 获取文件大小
     *
     * @param fileName 文件名
     * @return 文件大小（字节）
     */
    @Nullable
    Long getFileSize(String fileName);
}
```

**Step 2: 提交修改**

运行:
```bash
git add src/main/java/top/enderliquid/audioflow/manager/OSSManager.java
git commit -m "添加OSSManager接口"
```

---

### Task 10: 创建OSSManagerImpl

**Files:**
- Create: `src/main/java/top/enderliquid/audioflow/manager/impl/OSSManagerImpl.java`
- Test: 查找或创建OSSManagerImplTest.java

**Step 1: 创建OSSManagerImpl文件**

基于现有的S3StorageStrategy.java改造：
- 删除implements FileStorageStrategy，改为implements OSSManager
- 删除getType()方法
- 保留@Value注解配置的endpoint、region、accessKey、secretKey、bucketName、presignedUrlExpirationSeconds
- 保留s3Client和s3Presigner
- 保留init()和destroy()方法
- 改造save()方法为generatePresignedPostUrl()，生成预签名POST URL（使用S3Presigner.presignPutObject）
- 改造getUrl()方法为getPresignedGetUrl()
- 改造delete()方法为deleteFile()
- 新增checkFileExists()方法（使用HeadObjectRequest检查文件存在）
- 新增getFileInputStream()方法（使用GetObjectRequest获取文件流）
- 新增getFileSize()方法（从HeadObjectResponse获取ContentLength）

参考现有S3StorageStrategy.java:147-166的getUrl实现，改用presignPutObject生成POST URL。

**Step 2: 提交修改**

运行:
```bash
git add src/main/java/top/enderliquid/audioflow/manager/impl/OSSManagerImpl.java
git commit -m "添加OSSManagerImpl实现"
```

---

### Task 11: 修改SongManager接口

**Files:**
- Modify: `src/main/java/top/enderliquid/audioflow/manager/SongManager.java`
- Test: 无

**Step 1: 读取SongManager接口文件**

使用Read工具读取SongManager.java文件

**Step 2: 添加新方法**

在接口中添加：
```java
List<Song> listByStatusAndBeforeTime(String status, LocalDateTime time);
```

添加必要的import：
```java
import java.time.LocalDateTime;
import java.util.List;
```

**Step 3: 提交修改**

运行:
```bash
git add src/main/java/top/enderliquid/audioflow/manager/SongManager.java
git commit -m "SongManager新增listByStatusAndBeforeTime方法"
```

---

### Task 12: 实现SongManagerImpl新方法

**Files:**
- Modify: `src/main/java/top/enderliquid/audioflow/manager/impl/SongManagerImpl.java`
- Test: 无

**Step 1: 读取SongManagerImpl文件**

使用Read工具读取SongManagerImpl.java文件

**Step 2: 实现listByStatusAndBeforeTime方法**

添加方法实现：
```java
@Override
public List<Song> listByStatusAndBeforeTime(String status, LocalDateTime time) {
    LambdaQueryWrapper<Song> queryWrapper = new LambdaQueryWrapper<>();
    queryWrapper.eq(Song::getStatus, status)
            .lt(Song::getCreateTime, time);
    return this.list(queryWrapper);
}
```

添加必要的import：
```java
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.time.LocalDateTime;
import java.util.List;
```

**Step 3: 提交修改**

运行:
```bash
git add src/main/java/top/enderliquid/audioflow/manager/impl/SongManagerImpl.java
git commit -m "SongManagerImpl实现listByStatusAndBeforeTime方法"
```

---

### Task 13: 修改SongService接口

**Files:**
- Modify: `src/main/java/top/enderliquid/audioflow/service/SongService.java`
- Test: 无

**Step 1: 读取SongService接口文件**

使用Read工具读取SongService.java文件

**Step 2: 添加新方法**

在接口中添加：
```java
SongUploadPrepareVO prepareUpload(SongPrepareUploadDTO dto, Long userId);
SongVO completeUpload(SongCompleteUploadDTO dto, Long userId);
```

添加必要的import：
```java
import top.enderliquid.audioflow.dto.request.song.SongCompleteUploadDTO;
import top.enderliquid.audioflow.dto.request.song.SongPrepareUploadDTO;
import top.enderliquid.audioflow.dto.response.SongUploadPrepareVO;
```

**Step 3: 提交修改**

运行:
```bash
git add src/main/java/top/enderliquid/audioflow/service/SongService.java
git commit -m "SongService新增prepareUpload和completeUpload方法"
```

---

### Task 14: 实现SongServiceImpl新方法

**Files:**
- Modify: `src/main/java/top/enderliquid/audioflow/service/impl/SongServiceImpl.java`
- Test: 查找或创建SongServiceImplTest.java

**Step 1: 读取SongServiceImpl文件**

使用Read工具读取SongServiceImpl.java文件

**Step 2: 修改依赖注入**

删除FileManager注入，添加OSSManager注入：
```java
@Autowired
private OSSManager ossManager;
```

添加import：
```java
import top.enderliquid.audioflow.manager.OSSManager;
```

删除import：
```java
import top.enderliquid.audioflow.manager.FileManager;
```

**Step 3: 实现prepareUpload方法**

添加方法实现：
```java
@Override
public SongUploadPrepareVO prepareUpload(SongPrepareUploadDTO dto, Long userId) {
    log.info("请求准备上传歌曲，用户ID: {}", userId);
    // 检查用户是否存在
    User uploader = userManager.getById(userId);
    if (uploader == null) {
        throw new BusinessException("用户不存在");
    }
    // 验证mimetype
    String extension = MIME_TYPE_TO_EXTENSION_MAP.get(dto.getMimetype());
    if (extension == null) {
        throw new BusinessException("不支持该文件类型");
    }
    // 设置默认名称
    String name = dto.getName();
    if (name == null) {
        name = String.valueOf(IdWorker.getId());
    }
    // 保存歌曲信息（id=null, size=0, duration=null, status='UPLOADING'）
    Song song = new Song();
    song.setName(name);
    song.setDescription(dto.getDescription());
    song.setSize(0L);
    song.setDuration(null);
    song.setUploaderId(userId);
    song.setStatus(SongStatus.UPLOADING.name());
    if (!songManager.save(song)) {
        throw new BusinessException("歌曲信息保存失败");
    }
    // 生成文件名
    String fileName = song.getId() + "." + extension;
    song.setFileName(fileName);
    if (!songManager.updateById(song)) {
        throw new BusinessException("文件名更新失败");
    }
    // 生成预签名URL
    String uploadUrl = ossManager.generatePresignedPostUrl(fileName, Duration.ofMinutes(15));
    if (uploadUrl == null) {
        throw new BusinessException("生成上传URL失败");
    }
    // 返回准备结果
    SongUploadPrepareVO prepareVO = new SongUploadPrepareVO();
    prepareVO.setId(song.getId());
    prepareVO.setFileName(fileName);
    prepareVO.setUploadUrl(uploadUrl);
    log.info("准备上传歌曲成功，歌曲ID: {}, 文件名: {}", song.getId(), fileName);
    return prepareVO;
}
```

**Step 4: 实现completeUpload方法**

添加方法实现：
```java
@Override
public SongVO completeUpload(SongCompleteUploadDTO dto, Long userId) {
    log.info("请求完成上传歌曲，用户ID: {}, 歌曲ID: {}", userId, dto.getSongId());
    // 查询歌曲
    Song song = songManager.getById(dto.getSongId());
    if (song == null) {
        throw new BusinessException("歌曲不存在");
    }
    // 验证状态
    if (!SongStatus.UPLOADING.name().equals(song.getStatus())) {
        throw new BusinessException("歌曲状态异常");
    }
    // 验证上传者
    if (!song.getUploaderId().equals(userId)) {
        throw new BusinessException("无权操作");
    }
    // 检查文件是否存在OSS
    if (!ossManager.checkFileExists(song.getFileName())) {
        songManager.removeById(song);
        throw new BusinessException("上传文件不存在");
    }
    // 获取文件流并验证
    InputStream inputStream = ossManager.getFileInputStream(song.getFileName());
    if (inputStream == null) {
        throw new BusinessException("获取文件失败");
    }
    // 检测实际mimetype
    String actualMimeType;
    try {
        actualMimeType = TIKA.detect(inputStream);
    } catch (IOException e) {
        throw new BusinessException("无法获取文件类型", e);
    }
    // 关闭流
    try {
        inputStream.close();
    } catch (IOException e) {
        log.error("关闭文件流失败", e);
    }
    if (actualMimeType == null) {
        throw new BusinessException("无法获取文件类型");
    }
    // 获取实际后缀名
    String actualExtension = MIME_TYPE_TO_EXTENSION_MAP.get(actualMimeType);
    if (actualExtension == null) {
        // 删除数据库记录和OSS文件
        songManager.removeById(song);
        ossManager.deleteFile(song.getFileName());
        throw new BusinessException("文件类型不支持");
    }
    // 对比后缀名
    String expectedExtension = song.getFileName().substring(song.getFileName().lastIndexOf('.') + 1);
    if (!actualExtension.equals(expectedExtension)) {
        // 删除数据库记录和OSS文件
        songManager.removeById(song);
        ossManager.deleteFile(song.getFileName());
        throw new BusinessException("文件类型与后缀名不匹配");
    }
    // 获取文件大小和时长
    Long fileSize = ossManager.getFileSize(song.getFileName());
    if (fileSize == null) {
        throw new BusinessException("获取文件大小失败");
    }
    inputStream = ossManager.getFileInputStream(song.getFileName());
    if (inputStream == null) {
        throw new BusinessException("获取文件失败");
    }
    Long duration = getAudioDurationInMills(inputStream);
    try {
        inputStream.close();
    } catch (IOException e) {
        log.error("关闭文件流失败", e);
    }
    if (duration == null) {
        log.warn("解析歌曲持续时长失败");
        duration = 0L;
    }
    // 更新歌曲信息
    song.setSize(fileSize);
    song.setDuration(duration);
    song.setStatus(SongStatus.NORMAL.name());
    if (!songManager.updateById(song)) {
        throw new BusinessException("歌曲信息更新失败");
    }
    // 返回歌曲信息
    SongVO songVO = new SongVO();
    BeanUtils.copyProperties(song, songVO);
    songVO.setUploaderName(uploader.getName());
    log.info("完成上传歌曲成功，歌曲ID: {}", song.getId());
    return songVO;
}
```

**Step 5: 修改saveSong方法**

将FileManager的调用改为OSSManager：
- 删除fileManager.save(fileName, inputStream, mimeType)调用
- 删除fileManager.delete(fileName, sourceType)调用
- getSongUrl方法中将fileManager.getUrl()改为ossManager.getPresignedGetUrl()
- removeSong方法中将fileManager.delete()改为ossManager.deleteFile()

**Step 6: 添加必要的import**

添加：
```java
import top.enderliquid.audioflow.common.enums.SongStatus;
import top.enderliquid.audioflow.dto.request.song.SongCompleteUploadDTO;
import top.enderliquid.audioflow.dto.request.song.SongPrepareUploadDTO;
import top.enderliquid.audioflow.dto.response.SongUploadPrepareVO;
import top.enderliquid.audioflow.manager.OSSManager;
```

删除：
```java
import top.enderliquid.audioflow.manager.FileManager;
```

**Step 7: 提交修改**

运行:
```bash
git add src/main/java/top/enderliquid/audioflow/service/impl/SongServiceImpl.java
git commit -m "SongServiceImpl实现prepareUpload和completeUpload方法，修改相关方法使用OSSManager"
```

---

### Task 15: 修改SongController

**Files:**
- Modify: `src/main/java/top/enderliquid/audioflow/controller/SongController.java`
- Test: 查找或创建SongControllerTest.java

**Step 1: 读取SongController文件**

使用Read工具读取SongController.java文件

**Step 2: 添加prepareUpload接口**

在uploadSong方法后添加：
```java
@SaCheckLogin
@PostMapping("/prepare")
@RateLimit(
        refillRate = "3/60",
        capacity = 3,
        limitType = LimitType.BOTH,
        message = "上传过于频繁，请稍后再试"
)
public HttpResponseBody<SongUploadPrepareVO> prepareUpload(@Valid @RequestBody SongPrepareUploadDTO dto) {
    long userId = StpUtil.getLoginIdAsLong();
    SongUploadPrepareVO prepareVO = songService.prepareUpload(dto, userId);
    return HttpResponseBody.ok(prepareVO);
}
```

**Step 3: 添加completeUpload接口**

在prepareUpload方法后添加：
```java
@SaCheckLogin
@PostMapping("/complete")
public HttpResponseBody<SongVO> completeUpload(@Valid @RequestBody SongCompleteUploadDTO dto) {
    long userId = StpUtil.getLoginIdAsLong();
    SongVO songVO = songService.completeUpload(dto, userId);
    return HttpResponseBody.ok(songVO);
}
```

**Step 4: 修改或删除uploadSong接口**

可以选择删除原有uploadSong接口，或者保留用于向后兼容。如果保留，需要修改实现逻辑。

**Step 5: 添加必要的import**

添加：
```java
import top.enderliquid.audioflow.dto.request.song.SongCompleteUploadDTO;
import top.enderliquid.audioflow.dto.request.song.SongPrepareUploadDTO;
import top.enderliquid.audioflow.dto.response.SongUploadPrepareVO;
```

**Step 6: 提交修改**

运行:
```bash
git add src/main/java/top/enderliquid/audioflow/controller/SongController.java
git commit -m "SongController新增prepareUpload和completeUpload接口"
```

---

### Task 16: 创建定时清理任务

**Files:**
- Create: `src/main/java/top/enderliquid/audioflow/task/SongUploadCleanupTask.java`
- Test: 无

**Step 1: 创建SongUploadCleanupTask文件**

```java
package top.enderliquid.audioflow.task;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import top.enderliquid.audioflow.entity.Song;
import top.enderliquid.audioflow.manager.OSSManager;
import top.enderliquid.audioflow.manager.SongManager;
import top.enderliquid.audioflow.common.enums.SongStatus;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
public class SongUploadCleanupTask {

    @Autowired
    private SongManager songManager;

    @Autowired
    private OSSManager ossManager;

    /**
     * 每小时清理一次超过1小时的UPLOADING状态记录
     */
    @Scheduled(cron = "0 0 * * * ?")
    public void cleanupExpiredUploads() {
        log.info("开始清理超时上传记录");
        LocalDateTime cutoffTime = LocalDateTime.now().minusHours(1);
        List<Song> expiredSongs = songManager.listByStatusAndBeforeTime(
                SongStatus.UPLOADING.name(), cutoffTime);
        if (expiredSongs == null || expiredSongs.isEmpty()) {
            log.info("没有需要清理的超时上传记录");
            return;
        }
        int cleanedCount = 0;
        for (Song song : expiredSongs) {
            // 删除OSS文件
            if (ossManager.checkFileExists(song.getFileName())) {
                ossManager.deleteFile(song.getFileName());
            }
            // 删除数据库记录
            songManager.removeById(song);
            cleanedCount++;
        }
        log.info("清理超时上传记录完成，共清理{}条记录", cleanedCount);
    }
}
```

**Step 2: 提交修改**

运行:
```bash
git add src/main/java/top/enderliquid/audioflow/task/SongUploadCleanupTask.java
git commit -m "添加SongUploadCleanupTask定时清理任务"
```

---

### Task 17: 启用Spring定时任务

**Files:**
- Modify: 查找主应用类（带@SpringBootApplication注解的类）
- Test: 无

**Step 1: 查找主应用类**

运行: `find . -name "*Application.java" -type f | grep -E "src/main/java"`
预期: 找到主应用类

**Step 2: 读取主应用类**

使用Read工具读取找到的主应用类

**Step 3: 添加@EnableScheduling注解**

在类上添加@EnableScheduling注解：
```java
@EnableScheduling
@SpringBootApplication
public class AudioFlowApplication {
    // ...
}
```

**Step 4: 提交修改**

运行:
```bash
git add <主应用类路径>
git commit -m "启用Spring定时任务"
```

---

### Task 18: 修改application.properties配置

**Files:**
- Modify: `src/main/resources/application.properties`
- Test: 无

**Step 1: 读取application.properties文件**

使用Read工具读取application.properties文件

**Step 2: 删除不需要的配置**

删除以下配置：
```properties
file.storage.active=s3
file.storage.local.storage-dir=...
file.storage.local.url-prefix=...
```

**Step 3: 提交修改**

运行:
```bash
git add src/main/resources/application.properties
git commit -m "删除不需要的文件存储配置"
```

---

### Task 19: 删除FileManager相关文件

**Files:**
- Delete: `src/main/java/top/enderliquid/audioflow/manager/FileManager.java`
- Delete: `src/main/java/top/enderliquid/audioflow/manager/impl/FileManagerImpl.java`
- Delete: `src/main/java/top/enderliquid/audioflow/manager/strategy/file/FileStorageStrategy.java`
- Delete: `src/main/java/top/enderliquid/audioflow/manager/strategy/file/LocalStorageStrategy.java`
- Delete: `src/main/java/top/enderliquid/audioflow/manager/strategy/file/S3StorageStrategy.java`
- Delete: `src/main/java/top/enderliquid/audioflow/manager/strategy/file/`目录（如果为空）
- Delete: `src/main/java/top/enderliquid/audioflow/manager/strategy/file/`目录
- Delete: `src/main/java/top/enderliquid/audioflow/manager/strategy/`目录（如果为空）

**Step 1: 删除FileManager相关文件**

运行:
```bash
rm "src/main/java/top/enderliquid/audioflow/manager/FileManager.java"
rm "src/main/java/top/enderliquid/audioflow/manager/impl/FileManagerImpl.java"
rm "src/main/java/top/enderliquid/audioflow/manager/strategy/file/FileStorageStrategy.java"
rm "src/main/java/top/enderliquid/audioflow/manager/strategy/file/LocalStorageStrategy.java"
rm "src/main/java/top/enderliquid/audioflow/manager/strategy/file/S3StorageStrategy.java"
rmdir "src/main/java/top/enderliquid/audioflow/manager/strategy/file" 2>/dev/null || true
rmdir "src/main/java/top/enderliquid/audioflow/manager/strategy" 2>/dev/null || true
```

**Step 2: 提交修改**

运行:
```bash
git add -u
git commit -m "删除FileManager策略模式相关代码"
```

---

### Task 20: 编译项目验证

**Files:**
- 无
- Test: 无

**Step 1: 清理并编译项目**

运行: `./mvnw clean compile`
预期: 编译成功，无错误

**Step 2: 如果编译失败，修复错误并提交**

根据错误信息修复代码，然后提交修复：
```bash
git add -A
git commit -m "修复编译错误"
```

---

### Task 21: 运行测试

**Files:**
- 无
- Test: 所有测试文件

**Step 1: 运行所有测试**

运行: `./mvnw test`
预期: 所有测试通过

**Step 2: 如果有测试失败，修复测试**

根据失败的测试修复代码或测试用例，然后提交修复：
```bash
git add -A
git commit -m "修复测试失败"
```

---

### Task 22: 最终验证

**Files:**
- 无
- Test: 无

**Step 1: 构建项目**

运行: `./mvnw clean package -DskipTests`
预期: 构建成功

**Step 2: 检查git状态**

运行: `git status`
预期: 工作区干净，所有修改已提交

**Step 3: 查看提交历史**

运行: `git log --oneline -20`
预期: 看到所有提交记录

---

## 注意事项

1. **TDD原则**：每个任务都应先编写测试，再实现代码（虽然本计划中未显式列出测试任务，但在实际执行时应该遵循）
2. **频繁提交**：每个Task完成后立即提交，保持提交历史清晰
3. **代码风格**：遵循项目现有代码风格，所有注释使用中文
4. **类型声明**：禁止使用var关键字，所有变量使用显式类型声明
5. **日志格式**：遵循项目日志格式规范，入口日志、成功日志、错误日志分别记录

## 相关技能参考

- @writing-plans - 本计划的编写技能
- @executing-plans - 执行此计划时使用的技能
- @test-driven-development - TDD开发流程参考
- @systematic-debugging - 遇到问题时的调试技能