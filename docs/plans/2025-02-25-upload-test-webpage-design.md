# 上传测试网页设计文档

## 目标
创建一个独立的HTML测试网页，用于模拟前端参与完整的三步文件上传流程（prepare → OSS直传 → complete），方便项目测试。

## 技术方案

### 技术栈
- 纯静态HTML + 原生JavaScript
- Fetch API进行HTTP请求
- 无外部依赖（独立文件）

### 部署方式
- 独立HTML文件：`AudioFlow/upload-test.html`
- 通过浏览器直接打开访问
- 支持可配置的后端服务地址

## 功能需求

### 登录功能
- 邮箱和密码输入框
- 调用 `POST /api/sessions` 登录接口
- 保存返回的 satoken cookie
- 显示登录状态

### 上传功能
- 歌曲名称输入框（可选）
- 描述输入框（可选）
- 文件类型下拉选择（支持的音频格式）
- 文件选择按钮
- 开始上传按钮

### 上传流程
完整的三步上传流程：
1. 登录获取 satoken cookie
2. 调用 `/api/songs/prepare` 获取预签名URL和songId
3. 使用预签名URL直接上传文件到OSS
4. 调用 `/api/songs/complete` 通知后端完成上传

### 进度显示
- 实时显示四个步骤的状态（未开始、进行中、成功、失败）
- 状态使用颜色和符号区分
- 显示详细的错误信息（如果上传失败）

### 结果展示
- 上传成功后显示歌曲详情：
  - 歌曲ID
  - 文件名
  - 时长
  - 大小
  - 上传者信息

## 界面设计

### 最简界面风格
纯文本显示，清晰的输入框和按钮，无多余装饰。

### 界面布局
```
┌─────────────────────────────────────┐
│ 后端地址: [__________] [连接按钮]    │
├─────────────────────────────────────┤
│ 登录区域                              │
│   邮箱:    [_____________________]   │
│   密码:    [_____________________]   │
│         [登录]                        │
│ 状态: 未登录                           │
├─────────────────────────────────────┤
│ 上传区域                              │
│   歌曲名称:   [_______________]       │
│   描述:       [_______________]       │
│   文件类型:   [选择 ▼]              │
│   选择文件:   [选择文件按钮]         │
│              (已选: song.mp3)        │
│         [开始上传]                    │
├─────────────────────────────────────┤
│ 上传进度                              │
│ ▓ 1. 登录 成功 ✓                      │
│ ▓ 2. 准备上传 成功 ✓                 │
│ ▓ 3. 上传到OSS 进行中...              │
│ ▓ 4. 完成验证 等待开始                │
├─────────────────────────────────────┤
│ 结果详情                              │
│ 歌曲ID: 1234567890                   │
│ 文件名: 1234567890.mp3               │
│ 时长: 3:45                           │
│ 大小: 5.2 MB                         │
└─────────────────────────────────────┘
```

### 状态设计
- 未开始：灰色
- 进行中：黄色/闪烁效果
- 成功：绿色 ✓
- 失败：红色 ✕

## API接口设计

### 登录接口
- **URL**: `POST {baseURL}/api/sessions`
- **请求**:
  ```json
  {
    "email": "user@example.com",
    "password": "password123"
  }
  ```
- **响应**:
  ```json
  {
    "success": true,
    "message": "登录成功",
    "data": {
      "id": 123,
      "email": "user@example.com",
      "name": "用户名",
      "role": "USER"
    }
  }
  ```
- **Cookie**: satoken（自动设置）

### 准备上传接口
- **URL**: `POST {baseURL}/api/songs/prepare`
- **Headers**: Cookie: satoken=xxx
- **请求**:
  ```json
  {
    "mimeType": "audio/mpeg",
    "name": "歌曲名称",
    "description": "歌曲描述"
  }
  ```
- **响应**:
  ```json
  {
    "success": true,
    "message": "",
    "data": {
      "id": 1234567890,
      "fileName": "1234567890.mp3",
      "uploadUrl": "https://..."
    }
  }
  ```

### OSS直传
- **URL**: PUT {uploadUrl}（预签名URL）
- **Headers**: Content-Type: {mimeType}
- **Body**: 文件二进制数据

### 完成上传接口
- **URL**: `POST {baseURL}/api/songs/complete`
- **Headers**: Cookie: satoken=xxx
- **请求**:
  ```json
  {
    "songId": 1234567890
  }
  ```
- **响应**:
  ```json
  {
    "success": true,
    "message": "",
    "data": {
      "id": 1234567890,
      "name": "歌曲名称",
      "description": "歌曲描述",
      "size": 5242880,
      "duration": 225000,
      "uploaderId": 123,
      "uploaderName": "用户名",
      "status": "NORMAL",
      "createTime": "2025-02-25T10:30:00"
    }
  }
  ```

## 数据流

### 完整上传流程
```
1. 用户填写表单（邮箱、密码、歌曲信息、文件）
   ↓
2. 点击"登录"
   → POST {baseURL}/api/sessions
   → Body: { "email": "...", "password": "..." }
   → 保存返回的 satoken cookie
   → 更新状态："登录成功"
   ↓
3. 点击"开始上传"
   → POST {baseURL}/api/songs/prepare
   → Headers: Cookie: satoken=xxx
   → Body: { "mimeType": "audio/mpeg", "name": "xxx", "description": "xxx" }
   → 返回: { "id": 123, "fileName": "123.mp3", "uploadUrl": "..." }
   → 更新状态："准备上传成功"
   ↓
4. PUT上传到OSS
   → PUT {uploadUrl} (presigned URL)
   → Headers: Content-Type: {mimeType}
   → Body: {文件二进制数据}
   → 更新状态："上传到OSS成功"
   ↓
5. 完成验证
   → POST {baseURL}/api/songs/complete
   → Headers: Cookie: satoken=xxx
   → Body: { "songId": 123 }
   → 返回完整的 SongVO 信息
   → 更新状态："完成验证成功"
   → 显示结果详情（ID、时长、大小）
```

## 错误处理

### 错误类型
1. 登录失败（密码错误、账号不存在）
2. 准备上传失败（文件类型不支持、参数错误）
3. OSS上传失败（网络问题、权限问题）
4. 完成验证失败（文件不存在、文件类型不匹配）

### 错误显示
- 在对应步骤的进度区域显示红色 ✕
- 显示详细的错误消息
- 允许用户重试

## CORS配置

### 后端配置
在 `WebMvcConfig.java` 添加CORS支持：

```java
@Override
public void addCorsMappings(CorsRegistry registry) {
    registry.addMapping("/**")
            .allowedOriginPatterns("*")
            .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
            .allowedHeaders("*")
            .allowCredentials(true)
            .maxAge(3600);
}
```

### 支持的文件类型
- MP3: audio/mpeg, audio/mp3
- WAV: audio/wav, audio/x-wav, audio/vnd.wave
- OGG: audio/ogg, application/ogg
- FLAC: audio/flac, audio/x-flac

## 文件清单

### 需要创建的文件
- `upload-test.html` - 测试网页主文件

### 需要修改的文件
- `src/main/java/top/enderliquid/audioflow/common/config/WebMvcConfig.java` - 添加CORS配置

## 测试场景

1. 正常上传流程：登录 → prepare → OSS直传 → complete → 成功
2. 登录失败场景：错误的邮箱或密码
3. 文件类型不支持场景：选择不支持的文件类型
4. 网络错误场景：OSS上传失败
5. 文件验证失败场景：上传了格式不符的文件

## 技术约束

- 无外部CSS/JS库依赖
- 纯原生实现
- 最小化代码复杂度
- 遵循YAGNI原则（只实现必需功能）
