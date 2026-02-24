# 歌曲上传流程重构设计文档

## 目标

将歌曲上传从前端->后端->OSS的文件传输链，改为前端发request找后端获取临时上传URL，然后前端直传OSS的流程。取消策略模式的FileManager，改用专门管理S3的OSSManager。

## 架构变更

### 删除组件

- FileManager接口及实现（FileManagerImpl.java）
- FileStorageStrategy接口（FileStorageStrategy.java）
- LocalStorageStrategy实现（LocalStorageStrategy.java）
- S3StorageStrategy实现（S3StorageStrategy.java）
- 原有的SongSaveDTO（SongSaveDTO.java，包含MultipartFile的DTO）

### 新增组件

- OSSManager：直接管理S3操作
  - 生成预签名上传URL（POST Policy）
  - 检查文件存在
  - 获取文件InputStream
  - 删除文件
  - 获取预签名访问URL（播放用）
- SongStatus枚举：歌曲状态（UPLOADING/NORMAL）
- SongPrepareUploadDTO：上传准备请求（mimetype, name, description）
- SongCompleteUploadDTO：上传完成请求（songId）
- SongUploadPrepareVO：上传准备响应（id, fileName, uploadUrl）
- SongUploadCleanupTask：定时清理任务（每小时执行一次，清理超过1小时的UPLOADING记录）

### 修改组件

- Song实体：添加status字段，删除sourceType字段
- SongController：新增prepareUpload和completeUpload接口，删除或修改原有upload接口
- SongService：新增prepareUpload和completeUpload方法
- SongManager：新增listByStatusAndBeforeTime方法
- SongBO：适配新字段（添加status，删除sourceType）
- SongVO：适配新字段（添加status，删除sourceType）
- application.properties：删除本地存储配置，删除策略选择配置

## 数据库变更

**song表：**

```sql
ALTER TABLE song
ADD COLUMN status VARCHAR(20) DEFAULT 'UPLOADING',
DROP COLUMN source_type;
```

直接修改create.sql即可，无需数据迁移。

## 数据流设计

### 上传流程（三步走）

#### 第一步：上传准备（POST /api/songs/prepare）

1. 前端请求：SongPrepareUploadDTO（mimetype, name, description）
2. Service层校验：
   - mimetype是否在支持列表中（audio/mpeg, audio/wav, audio/ogg, audio/flac等）
   - 根据mimetype获取文件后缀名（.mp3, .wav, .ogg, .flac等）
3. 保存歌曲信息到数据库（id=null, size=0, duration=null, status='UPLOADING'）
4. MyBatis-Plus自动回填id
5. 生成完整文件名：`{id}.{extension}`
6. 更新fileName到数据库
7. OSSManager生成POST Policy预签名URL：
   - 限制bucket和key（文件路径）
   - 设置过期时间（15分钟）
8. 返回：SongUploadPrepareVO（id, fileName, uploadUrl）

#### 第二步：前端直传OSS

1. 前端使用返回的uploadUrl通过POST直接上传文件到OSS
2. 文件存储路径：`{bucket}/{fileName}`
3. 前端上传成功后，携带songId调用完成接口

#### 第三步：上传完成（POST /api/songs/complete）

1. 前端请求：SongCompleteUploadDTO（songId）
2. Service层校验：
   - 歌曲是否存在且status='UPLOADING'
   - 上传者ID是否匹配请求发起者（Sa-Token获取）
3. OSSManager检查文件是否真的存在于OSS
4. OSSManager获取文件InputStream
5. 使用Tika检测实际mimetype
6. 对比两个mimetype对应的后缀名是否一致：
   - 如果一致：
     - Tika获取duration
     - Tika获取文件size
     - 更新数据库（size, duration, status='NORMAL'）
     - 返回歌曲VO
   - 如果不一致：
     - 删除数据库记录
     - OSSManager删除OSS文件
     - 抛出BusinessException("文件类型与后缀名不匹配")

### 定时清理流程

**SongUploadCleanupTask（每小时执行一次）：**

1. 查询创建时间超过1小时且status='UPLOADING'的所有歌曲
2. 对每条记录：
   - 检查文件是否存在于OSS
   - 如果存在，OSSManager删除OSS文件
   - 删除数据库记录
3. 记录清理日志

## 组件设计

### OSSManager接口

```java
public interface OSSManager {
    /**
     * 生成预签名POST上传URL（带Policy限制）
     *
     * @param fileName 文件名
     * @param expiration 过期时间
     * @return 预签名URL
     */
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
    String getPresignedGetUrl(String fileName, Duration expiration);

    /**
     * 获取文件大小
     *
     * @param fileName 文件名
     * @return 文件大小（字节）
     */
    Long getFileSize(String fileName);
}
```

### OSSManagerImpl

基于现有的S3StorageStrategy改造，删除策略模式，只保留S3功能。使用AWS S3 SDK实现。

### SongStatus枚举

```java
public enum SongStatus {
    UPLOADING,
    NORMAL
}
```

### SongPrepareUploadDTO

```java
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

### SongCompleteUploadDTO

```java
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SongCompleteUploadDTO {
    @NotNull(message = "歌曲ID不能为空")
    private Long songId;
}
```

### SongUploadPrepareVO

```java
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SongUploadPrepareVO {
    private Long id;
    private String fileName;
    private String uploadUrl;
}
```

## 接口设计

### SongController新增接口

**POST /api/songs/prepare**
- 功能：准备上传，获取预签名URL
- 认证：@SaCheckLogin
- 限流：3次/分钟（refillRate="3/60", capacity=3）
- 请求体：SongPrepareUploadDTO（mimetype, name, description）
- 响应：HttpResponseBody<SongUploadPrepareVO>
  - id: 歌曲ID
  - fileName: 文件名（id.extension）
  - uploadUrl: 预签名上传URL

**POST /api/songs/complete**
- 功能：完成上传，验证文件
- 认证：@SaCheckLogin
- 请求体：SongCompleteUploadDTO（songId）
- 响应：HttpResponseBody<SongVO>（包含status）或错误信息

### SongService新增方法

```java
SongUploadPrepareVO prepareUpload(SongPrepareUploadDTO dto, Long userId);
SongVO completeUpload(SongCompleteUploadDTO dto, Long userId);
```

### SongManager新增方法

```java
List<Song> listByStatusAndBeforeTime(SongStatus status, LocalDateTime time);
```

## 错误处理

- mimetype不支持：抛出BusinessException("不支持该文件类型")
- 歌曲不存在或状态不符：抛出BusinessException("歌曲不存在或状态异常")
- 权限不足：抛出BusinessException("无权操作")
- 文件不存在OSS：抛出BusinessException("上传文件不存在")
- mimetype不匹配：抛出BusinessException("文件类型与后缀名不匹配")

## 配置变更

**application.properties**

删除：
```properties
file.storage.active=s3
file.storage.local.storage-dir=...
file.storage.local.url-prefix=...
```

保留：
```properties
file.storage.s3.endpoint=...
file.storage.s3.region=...
file.storage.s3.access-key=...
file.storage.s3.secret-key=...
file.storage.s3.bucket-name=...
file.storage.s3.presigned-url-expiration=3600
```

## 技术栈

- Java 21
- Spring Boot 3.5.10
- Sa-Token（认证）
- MyBatis-Plus（持久层）
- AWS S3 SDK（OSS操作）
- Apache Tika（文件类型检测和时长获取）
- Spring @Scheduled（定时任务）

## 影响范围

### 需要修改的文件

1. 数据库脚本：create.sql
2. 实体类：Song.java
3. 枚举：新增SongStatus.java
4. DTO：
   - 新增：SongPrepareUploadDTO.java, SongCompleteUploadDTO.java, SongUploadPrepareVO.java
   - 删除：SongSaveDTO.java（或保留用于其他用途）
5. BO：SongBO.java
6. VO：SongVO.java
7. Manager：
   - 删除：FileManager.java, FileManagerImpl.java, FileStorageStrategy.java, LocalStorageStrategy.java, S3StorageStrategy.java
   - 新增：OSSManager.java, OSSManagerImpl.java
   - 修改：SongManager.java（新增方法）
8. Mapper：SongMapper.java（可能需要新增查询方法）
9. Service：SongService.java（新增方法）
10. ServiceImpl：SongServiceImpl.java（实现新方法）
11. Controller：SongController.java（新增接口）
12. Task：新增SongUploadCleanupTask.java
13. 配置：application.properties（删除不需要的配置）

### 前端影响

前端需要修改上传逻辑：
1. 先调用POST /api/songs/prepare获取上传URL
2. 使用返回的uploadUrl直接上传文件到OSS
3. 上传成功后调用POST /api/songs/complete通知后端验证

## 测试策略

1. 单元测试：测试OSSManager各方法
2. 集成测试：测试三步上传流程
3. 错误场景测试：
   - 不支持的mimetype
   - 文件不存在OSS
   - mimetype不匹配
   - 权限不足
   - 定时清理任务
4. 性能测试：测试并发上传和定时清理性能