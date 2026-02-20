# AudioFlow API接口测试覆盖方案设计

**日期**: 2026-02-20
**目标**: 覆盖所有13个API接口的集成测试

---

## 一、需求概述

为AudioFlow项目创建完整的Controller层集成测试，覆盖以下所有API接口：

- **UserController** (3个接口): 注册、获取用户信息、修改密码
- **SongController** (8个接口): 上传、分页查询、删除、强制删除、获取信息、播放URL、更新、强制更新
- **SessionController** (2个接口): 登录、注销

当前测试覆盖率：0%

---

## 二、测试架构设计

### 2.1 项目目录结构

```
src/test/java/top/enderliquid/audioflow/
├── controller/                           # Controller层集成测试
│   ├── UserControllerTest.java          # UserController测试（3个接口）
│   ├── SessionControllerTest.java       # SessionController测试（2个接口）
│   └── SongControllerTest.java          # SongController测试（8个接口）
├── config/                              # 测试配置类
│   └── IntegrationTestConfig.java        # 测试配置（MockMvc等）
├── resources/                           # 测试资源
│   ├── application-test.properties     # 测试环境配置
│   └── audio/                           # 测试音频文件
│       └── test-song.mp3               # 测试用的MP3文件
└── common/                              # 测试工具类
    └── TestDataHelper.java              # 测试数据辅助类
```

### 2.2 测试基类 - BaseControllerTest

**职责**：
- 提供`MockMvc`实例用于发送HTTP请求
- 提供`ObjectMapper`用于JSON序列化/反序列化
- 提供Sa-Token Mock工具方法
- 提供通用的断言辅助方法

**注解**：
```java
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties")
```

**核心方法**：
- `mockLogin(Long userId)` - 模拟用户登录
- `mockAdminLogin(Long userId)` - 模拟管理员登录
- `toJson(T obj)` - JSON序列化
- `fromJson(String json, Class<T> clazz)` - JSON反序列化

---

## 三、测试配置（application-test.properties）

### 3.1 配置调整

| 配置项 | 生产值 | 测试值 | 说明 |
|--------|--------|--------|------|
| 数据库URL | `jdbc:mysql://127.0.0.1:3306/audioflow` | 同生产或独立测试库 | 使用真实MySQL |
| Redis配置 | 同生产 | 同生产 | 共享实例，使用Sa-Token Mock |
| 文件存储策略 | `s3` | `local` | 避免S3交互 |
| 本地存储路径 | `/upload/` | `./target/test-upload/` | 使用临时目录 |
| 日志级别 | INFO | WARN/ERROR | 减少测试日志 |

### 3.2 Sa-Token配置
测试环境中使用Sa-Token的Mock能力，不依赖真实token验证。

---

## 四、数据管理策略

### 4.1 测试数据清理

在`@BeforeEach`方法中按依赖顺序清理：
1. 删除`songs`表数据（song依赖user）
2. 删除`users`表数据
3. **注意**：不重置ID，项目中使用MyBatis-Plus雪花算法

### 4.2 测试数据初始化

创建`TestDataHelper`工具类，提供：
- `createTestUser()` - 创建测试用户
- `createTestSong(Long userId)` - 创建测试歌曲
- `cleanDatabase()` - 清理数据库

数据命名必须可预测：
- 用户测试数据用固定邮箱如`test_user@example.com`
- 歌曲测试数据用固定名称如`Test Song`

### 4.3 测试方法标准结构

```java
@BeforeEach
void setUp() {
    testDataHelper.cleanDatabase();
    testUser = testDataHelper.createTestUser();
    testSong = testDataHelper.createTestSong(testUser.getId());
    mockLogin(testUser.getId());
}
```

---

## 五、认证Mock策略

### 5.1 Sa-Token Mock方式

```java
// 普通用户登录
StpUtil.login(userId);

// 管理员登录
StpUtil.login(userId);
StpUtil.getSession().set("role", "ADMIN");
```

### 5.2 测试权限检查

- **未登录测试**：不调用`mockLogin()`
- **非管理员测试**：调用`mockLogin(userId)`
- **管理员测试**：调用`mockAdminLogin(userId)`

---

## 六、测试文件资源

### 6.1 文件要求

- **位置**：`src/test/resources/audio/`
- **格式**：支持的音频格式（MP3等）
- **大小**：建议100KB以内，缩短测试时间
- **提供方式**：用户手动提供测试文件

### 6.2 使用方式

```java
Resource audioResource = new ClassPathResource("audio/test-song.mp3");
File audioFile = audioResource.getFile();

MockMultipartFile file = new MockMultipartFile(
    "file",
    "test-song.mp3",
    "audio/mpeg",
    Files.readAllBytes(audioFile.toPath())
);
```

---

## 七、测试用例设计

### 7.1 测试覆盖范围

每个接口测试以下场景：
1. **成功场景**：正常参数，验证返回数据正确
2. **参数验证**：缺失必填字段、格式错误等
3. **权限验证**：未登录、角色不匹配
4. **业务异常**：重复注册、密码错误、资源不存在等

### 7.2 测试方法命名规范

```
should{操作}When{条件}
shouldThrow{异常}When{条件}
shouldReturn{结果}When{条件}
```

**示例**：
- `shouldRegisterSuccessfully`
- `shouldThrowBusinessExceptionWhenUserAlreadyExists`
- `shouldReturnSongListWhenQueryWithKeyword`

### 7.3 异常验证

**方式1：JUnit 5 assertThrows**
```java
assertThrows(NotLoginException.class, () -> {
    mockMvc.perform(get("/api/users/me"));
});
```

**方式2：MockMvc验证响应状态码**
```java
mockMvc.perform(get("/api/users/me"))
       .andExpect(status().isOk())
       .andExpect(jsonPath("$.code").value(错误码));
```

---

## 八、实施计划

### Phase 1：基础设施（优先级：高）
1. 创建测试目录结构
2. 创建`BaseControllerTest`基类
3. 创建`application-test.properties`
4. 创建`TestDataHelper`工具类
5. 准备测试音频文件

### Phase 2：基础功能测试（优先级：高）
1. `SessionControllerTest.java`（2个接口）
   - POST `/api/sessions` - 登录成功/失败
   - DELETE `/api/sessions/current` - 注销成功/未登录

2. `UserControllerTest.java`（3个接口）
   - POST `/api/users` - 注册成功/邮箱已存在/参数验证
   - GET `/api/users/me` - 获取成功/未登录
   - PATCH `/api/users/me/password` - 修改成功/密码错误/未登录

### Phase 3：核心业务测试（优先级：中）
1. `SongControllerTest` - 基础CRUD（5个接口）
   - POST `/api/songs` - 上传成功/未登录
   - GET `/api/songs` - 分页查询成功/空列表
   - GET `/api/songs/{id}` - 获取成功/不存在
   - GET `/api/songs/{id}/play` - 获取URL成功/不存在
   - DELETE `/api/songs/{id}` - 删除成功/无权限/不存在

### Phase 4：权限和更新接口（优先级：中）
1. `SongControllerTest` - 补充测试（3个接口）
   - PATCH `/api/songs/{id}` - 更新成功/无权限/不存在
   - DELETE `/api/songs/{id}/force` - 管理员删除成功/非管理员
   - PATCH `/api/songs/{id}/force` - 管理员更新成功/非管理员

---

## 九、技术栈

| 组件 | 技术 |
|------|------|
| 测试框架 | JUnit 5 |
| Spring测试 | `spring-boot-starter-test` |
| Servlet Mock | Spring `MockMvc`, `MockHttpServletRequest` |
| Mock框架 | Mockito |
| 序列化 | Jackson `ObjectMapper` |
| 认证Mock | Sa-Token Mock工具 |

---

## 十、验证标准

- ✅ 所有13个API接口均有对应测试方法
- ✅ 每个接口至少包含成功场景和1个异常场景
- ✅ 需要登录的接口测试未登录场景
- ✅ 管理员接口测试非管理员场景
- ✅ 所有测试用`mvn test`能通过

---

## 十一、后续优化建议

1. **代码覆盖率工具**：集成JaCoCo生成覆盖率报告
2. **CI/CD集成**：在GitHub Actions中运行测试
3. **测试数据隔离**：考虑使用Testcontainers实现完全隔离
4. **性能基准测试**：对关键接口进行性能测试

---

**附录：API接口清单**

| # | Controller | HTTP方法 | 路径 | 功能 | 权限要求 |
|---|------------|----------|------|------|----------|
| 1 | UserController | POST | `/api/users` | 用户注册 | 无 |
| 2 | UserController | GET | `/api/users/me` | 获取当前登录用户信息 | @SaCheckLogin |
| 3 | UserController | PATCH | `/api/users/me/password` | 更改用户密码 | @SaCheckLogin |
| 4 | SessionController | POST | `/api/sessions` | 用户登录 | 无 |
| 5 | SessionController | DELETE | `/api/sessions/current` | 用户注销 | @SaCheckLogin |
| 6 | SongController | POST | `/api/songs` | 上传歌曲 | @SaCheckLogin |
| 7 | SongController | GET | `/api/songs` | 分页查询/搜索歌曲 | 无 |
| 8 | SongController | DELETE | `/api/songs/{id}` | 删除自己的歌曲 | @SaCheckLogin |
| 9 | SongController | DELETE | `/api/songs/{id}/force` | 管理员强制删除歌曲 | @SaCheckLogin + ADMIN |
| 10 | SongController | GET | `/api/songs/{id}` | 获取歌曲信息 | 无 |
| 11 | SongController | GET | `/api/songs/{id}/play` | 获取歌曲播放URL | 无 |
| 12 | SongController | PATCH | `/api/songs/{id}` | 更新自己的歌曲信息 | @SaCheckLogin |
| 13 | SongController | PATCH | `/api/songs/{id}/force` | 管理员强制更新歌曲信息 | @SaCheckLogin + ADMIN |
