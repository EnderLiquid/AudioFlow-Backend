# 批量歌曲操作功能设计

## 概述

实现批量上传歌曲和批量删除歌曲功能，沿用现有的两步上传流程，支持部分成功返回详情。

## API 设计

### 1. 批量准备上传

```
POST /api/songs/batch-prepare
```

**请求体**：`SongBatchPrepareDTO`
```json
{
  "songs": [
    { "mimeType": "audio/mpeg", "name": "歌曲1", "description": "描述1" },
    { "mimeType": "audio/mpeg", "name": "歌曲2", "description": "描述2" }
  ]
}
```

**限制**：最多 10 首歌曲

**响应**：`SongBatchResultVO<SongUploadPrepareVO>`
```json
{
  "successList": [
    { "id": 1, "fileName": "1.mp3", "uploadUrl": "https://..." },
    { "id": 2, "fileName": "2.mp3", "uploadUrl": "https://..." }
  ],
  "failureList": [
    { "index": 2, "reason": "不支持该文件类型" }
  ],
  "successCount": 2,
  "failureCount": 1
}
```

### 2. 批量确认上传

```
POST /api/songs/batch-complete
```

**请求体**：`SongBatchCompleteDTO`
```json
{
  "songIds": [1, 2, 3]
}
```

**响应**：`SongBatchResultVO<SongVO>`
```json
{
  "successList": [
    { "id": 1, "name": "歌曲1", ... },
    { "id": 2, "name": "歌曲2", ... }
  ],
  "failureList": [
    { "index": 2, "id": 3, "reason": "上传文件不存在" }
  ],
  "successCount": 2,
  "failureCount": 1
}
```

### 3. 批量删除

```
POST /api/songs/batch
X-HTTP-Method-Override: DELETE
```

**请求体**：`SongBatchDeleteDTO`
```json
{
  "songIds": [1, 2, 3]
}
```

**响应**：`SongBatchResultVO<Long>`
```json
{
  "successList": [1, 2],
  "failureList": [
    { "index": 2, "id": 3, "reason": "无权删除他人上传的歌曲" }
  ],
  "successCount": 2,
  "failureCount": 1
}
```

## 新增类

### DTO 类

| 类名 | 包 | 用途 |
|------|-----|------|
| `SongBatchPrepareDTO` | dto.request.song | 批量准备上传请求 |
| `SongBatchCompleteDTO` | dto.request.song | 批量确认上传请求 |
| `SongBatchDeleteDTO` | dto.request.song | 批量删除请求 |
| `SongBatchResultVO<T>` | dto.response | 批量操作结果响应（泛型） |
| `BatchFailureItem` | dto.response | 失败项详情 |

### 类定义

```java
// SongBatchPrepareDTO
public class SongBatchPrepareDTO {
    @Valid
    @Size(min = 1, max = 10, message = "批量上传数量必须在1-10之间")
    private List<SongPrepareUploadDTO> songs;
}

// SongBatchCompleteDTO
public class SongBatchCompleteDTO {
    @NotEmpty(message = "歌曲ID列表不能为空")
    @Size(max = 10, message = "批量确认数量不能超过10")
    private List<Long> songIds;
}

// SongBatchDeleteDTO
public class SongBatchDeleteDTO {
    @NotEmpty(message = "歌曲ID列表不能为空")
    @Size(max = 10, message = "批量删除数量不能超过10")
    private List<Long> songIds;
}

// SongBatchResultVO<T>
public class SongBatchResultVO<T> {
    private List<T> successList;
    private List<BatchFailureItem> failureList;
    private int successCount;
    private int failureCount;
}

// BatchFailureItem
public class BatchFailureItem {
    private Integer index;    // 在请求列表中的索引
    private Long id;          // 相关ID（可选）
    private String reason;    // 失败原因
}
```

## 实现要点

### Service 层新增方法

```java
// SongService 接口
SongBatchResultVO<SongUploadPrepareVO> batchPrepareUpload(SongBatchPrepareDTO dto, Long userId);
SongBatchResultVO<SongVO> batchCompleteUpload(SongBatchCompleteDTO dto, Long userId);
SongBatchResultVO<Long> batchRemoveSongs(SongBatchDeleteDTO dto, Long userId);
```

### 业务逻辑

1. **批量准备上传**
   - 遍历请求列表，复用现有 `prepareUpload` 逻辑
   - 记录每项成功/失败结果
   - 失败不影响其他项的处理

2. **批量确认上传**
   - 遍历 songId 列表，复用现有 `completeUpload` 逻辑
   - 记录每项成功/失败结果

3. **批量删除**
   - 遍历 songId 列表，复用现有 `removeSong` 逻辑
   - 记录每项成功/失败结果

### 权限控制

- 批量准备上传：需要登录
- 批量确认上传：需要登录，验证歌曲所有者
- 批量删除：需要登录，管理员可删除所有，普通用户仅能删除自己的

### 限流控制

沿用现有的 `@RateLimit` 注解，批量操作设置合理的限流参数。