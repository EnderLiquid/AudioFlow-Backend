# API接口测试覆盖实现计划

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**目标:** 为AudioFlow项目的13个API接口创建完整的集成测试，从基础设施搭建到测试实现，覆盖所有场景包括权限验证、参数验证和业务异常。

**架构:** 基于Spring Boot Test的集成测试，使用MockMvc模拟HTTP请求，Sa-Token Mock处理权限，真实MySQL数据库测试，BeforeEach清理和初始化测试数据。

**技术栈:** JUnit 5, Spring Boot Test, MockMvc, Mockito, Jackson ObjectMapper, Sa-Token Mock工具

---

## Phase 1: 基础设施搭建

### Task 1: 创建测试目录结构

**Files:**
- Create: `src/test/java/top/enderliquid/audioflow/controller/`
- Create: `src/test/java/top/enderliquid/audioflow/config/`
- Create: `src/test/java/top/enderliquid/audioflow/common/`
- Create: `src/test/resources/audio/`

**Step 1: 创建所有目录**

```bash
mkdir -p src/test/java/top/enderliquid/audioflow/controller
mkdir -p src/test/java/top/enderliquid/audioflow/config
mkdir -p src/test/java/top/enderliquid/audioflow/common
mkdir -p src/test/resources/audio
```

**Step 2: 验证目录创建成功**

```bash
ls -la src/test/java/top/enderliquid/audioflow/controller
ls -la src/test/resources/audio
```

**Expected:** 应显示空目录创建成功

**Step 3: 提交**

```bash
git add src/test/java/top/enderliquid/audioflow/controller src/test/java/top/enderliquid/audioflow/config src/test/java/top/enderliquid/audioflow/common src/test/resources/audio
git commit -m "test: 创建测试目录结构"
```

---

### Task 2: 创建测试配置文件

**Files:**
- Create: `src/test/resources/application-test.properties`

**Step 1: 创建application-test.properties**

```properties
# 应用名称
spring.application.name=AudioFlowTest

# 数据库配置 - 使用真实MySQL
spring.datasource.url=jdbc:mysql://127.0.0.1:3306/audioflow?useSSL=false&serverTimezone=UTC
spring.datasource.username=root
spring.datasource.password=${DB_PASSWORD:}
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

# MyBatis-Plus 配置
mybatis-plus.global-config.db-config.worker-id=${worker.id:0}
mybatis-plus.global-config.db-config.datacenter-id=${datacenter.id:0}
mybatis-plus.mapper-locations=classpath:mapper/*.xml

# Redis 配置（共享实例）
spring.data.redis.database=0
spring.data.redis.host=127.0.0.1
spring.data.redis.port=6379
spring.data.redis.password=${REDIS_PASSWORD:}

# 文件上传配置
spring.servlet.multipart.max-file-size=10MB
spring.servlet.multipart.max-request-size=20MB

# 文件保存配置 - 使用本地而非S3
spring.web.resources.add-mappings=true
file.storage.active=local
file.storage.local.storage-dir=./target/test-upload/
file.storage.local.url-prefix=http://127.0.0.1:8081/file/

# Sa-Token 配置（测试环境）
sa-token.token-name=satoken
sa-token.timeout=604800
sa-token.active-timeout=-1
sa-token.is-concurrent=true
sa-token.is-share=false
sa-token.token-style=uuid
sa-token.is-log=false

# 日志配置 - 减少测试日志输出
logging.level.root=WARN
logging.level.top.enderliquid.audioflow=WARN

# 异常处理配置
response.exception.expose-uncaught-exception-detail=true
```

**Step 2: 验证配置文件**

```bash
cat src/test/resources/application-test.properties
```

**Expected:** 显示上述配置内容

**Step 3: 提交**

```bash
git add src/test/resources/application-test.properties
git commit -m "test: 添加测试环境配置文件"
```

---

### Task 3: 创建测试基类BaseControllerTest

**Files:**
- Create: `src/test/java/top/enderliquid/audioflow/config/BaseControllerTest.java`

**Step 1: 创建BaseControllerTest基类**

```java
package top.enderliquid.audioflow.config;

import cn.dev33.satoken.stp.StpUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties")
@Slf4j
public abstract class BaseControllerTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @BeforeEach
    void clearSession() {
        StpUtil.logout();
    }

    protected void mockLogin(Long userId) {
        StpUtil.login(userId);
        log.debug("模拟登录用户ID: {}", userId);
    }

    protected void mockAdminLogin(Long userId) {
        StpUtil.login(userId);
        StpUtil.getSession().set("role", "ADMIN");
        log.debug("模拟管理员登录用户ID: {}", userId);
    }

    protected <T> String toJson(T obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("JSON序列化失败", e);
        }
    }
}
```

**Step 2: 验证基类编译**

```bash
./mvnw clean compile test-compile
```

**Expected:** BUILD SUCCESS

**Step 3: 提交**

```bash
git add src/test/java/top/enderliquid/audioflow/config/BaseControllerTest.java
git commit -m "test: 创建测试基类BaseControllerTest"
```

---

### Task 4: 创建测试数据辅助类TestDataHelper

**Files:**
- Create: `src/test/java/top/enderliquid/audioflow/common/TestDataHelper.java`

**Step 1: 创建TestDataHelper类**

首先需要了解实体类结构，先查看User和Song实体：

```bash
find src/main/java -name "User.java" -o -name "Song.java"
```

假设实体有以下字段，创建帮助类：

```java
package top.enderliquid.audioflow.common;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import top.enderliquid.audioflow.entity.User;
import top.enderliquid.audioflow.entity.Song;
import top.enderliquid.audioflow.manager.UserManager;
import top.enderliquid.audioflow.manager.SongManager;

@Component
public class TestDataHelper {

    @Autowired
    private UserManager userManager;

    @Autowired
    private SongManager songManager;

    public void cleanDatabase() {
        songManager.lambdaUpdate().remove();
        userManager.lambdaUpdate().remove();
    }

    public User createTestUser() {
        User user = new User();
        user.setNickname("test_user");
        user.setEmail("test_user@example.com");
        user.setPassword("test_password_123");
        userManager.save(user);
        return user;
    }

    public Song createTestSong(Long userId) {
        Song song = new Song();
        song.setTitle("Test Song");
        song.setArtist("Test Artist");
        song.setAlbum("Test Album");
        song.setUploaderId(userId);
        song.setFileKey("test-key");
        song.setDuration(180);
        songManager.save(song);
        return song;
    }
}
```

**Step 2: 运行编译验证**

```bash
./mvnw clean compile test-compile
```

**Expected:** BUILD SUCCESS

**Step 3: 提交**

```bash
git add src/test/java/top/enderliquid/audioflow/common/TestDataHelper.java
git commit -m "test: 创建测试数据辅助类TestDataHelper"
```

---

### Task 5: 准备测试音频文件

**Files:**
- User input required

**Step 1: 请求用户提供测试音频文件**

**提示:** 请提供一个小型的测试用MP3文件（建议100KB以内），用于测试POST /api/songs上传接口。

**Step 2: 放置文件到测试资源目录**

```bash
# 用户提供的文件命名为 test-song.mp3
# 移动到 src/test/resources/audio/ 目录
```

**Step 3: 验证文件存在**

```bash
ls -lh src/test/resources/audio/test-song.mp3
```

**Expected:** 显示文件信息且文件大小合理（< 100KB）

**Step 4: 提交**

```bash
git add src/test/resources/audio/test-song.mp3
git commit -m "test: 添加测试音频文件"
```

---

## Phase 2: SessionController测试实现

### Task 6: 创建SessionControllerTest基础结构

**Files:**
- Create: `src/test/java/top/enderliquid/audioflow/controller/SessionControllerTest.java`

**Step 1: 编写SessionControllerTest类结构**

```java
package top.enderliquid.audioflow.controller;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import top.enderliquid.audioflow.common.TestDataHelper;
import top.enderliquid.audioflow.config.BaseControllerTest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class SessionControllerTest extends BaseControllerTest {

    @Autowired
    private TestDataHelper testDataHelper;

    @BeforeEach
    void setUp() {
        testDataHelper.cleanDatabase();
    }

    // 测试方法将在后续任务中添加
}
```

**Step 2: 验证编译**

```bash
./mvnw test-compile
```

**Expected:** BUILD SUCCESS

**Step 3: 提交**

```bash
git add src/test/java/top/enderliquid/audioflow/controller/SessionControllerTest.java
git commit -m "test: 创建SessionControllerTest类结构"
```

---

### Task 7: 测试POST /api/sessions登录成功

**Files:**
- Modify: `src/test/java/top/enderliquid/audioflow/controller/SessionControllerTest.java`

**Step 1: 查看登录接口的DTO结构**

```bash
find src/main/java -name "*UserVerifyPasswordDTO.java"
cat $(find src/main/java -name "*UserVerifyPasswordDTO.java")
```

假设DTO有email和password字段。

**Step 2: 添加登录成功测试方法**

```java
@Test
void shouldLoginSuccessfullyWhenCredentialsCorrect() throws Exception {
    testDataHelper.createTestUser();

    String email = "test_user@example.com";
    String password = "test_password_123";

    String requestJson = String.format("{\"email\":\"%s\",\"password\":\"%s\"}", email, password);

    mockMvc.perform(post("/api/sessions")
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestJson))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.email").value(email))
            .andExpect(jsonPath("$.message").value("登录成功"));
}
```

**Step 3: 运行测试**

```bash
./mvnw test -Dtest=SessionControllerTest#shouldLoginSuccessfullyWhenCredentialsCorrect
```

**Expected:** FAIL（如果用户创建的密码需要加密处理）

**Step 4: 检查UserService实现了解密码加密逻辑**

密码可能在Service层加密存储，需要用正确逻辑测试。查看UserService：

```bash
find src/main/java -name "UserService.java"
```

**修正步骤:** 如果密码需要加密，调整测试代码使用正确密码逻辑。

**Step 5: 运行测试验证通过**

```bash
./mvnw test -Dtest=SessionControllerTest#shouldLoginSuccessfullyWhenCredentialsCorrect
```

**Expected:** PASS

**Step 6: 提交**

```bash
git add src/test/java/top/enderliquid/audioflow/controller/SessionControllerTest.java
git commit -m "test: 添加登录成功测试用例"
```

---

### Task 8: 测试POST /api/sessions登录失败（密码错误）

**Files:**
- Modify: `src/test/java/top/enderliquid/audioflow/controller/SessionControllerTest.java`

**Step 1: 添加密码错误测试方法**

```java
@Test
void shouldReturnErrorWhenPasswordIncorrect() throws Exception {
    testDataHelper.createTestUser();

    String email = "test_user@example.com";
    String wrongPassword = "wrong_password";

    String requestJson = String.format("{\"email\":\"%s\",\"password\":\"%s\"}", email, wrongPassword);

    mockMvc.perform(post("/api/sessions")
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestJson))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(非用户错误码))
            .andExpect(jsonPath("$.message").value(包含"密码"或"登录失败"));
}
```

**Step 2: 查看错误码定义**

```bash
grep -r "code\|error" src/main/java/top/enderliquid/audioflow/common/response/ | head -10
```

**调整:** 根据实际错误码和消息调整测试断言。

**Step 3: 运行测试**

```bash
./mvnw test -Dtest=SessionControllerTest#shouldReturnErrorWhenPasswordIncorrect
```

**Expected:** PASS

**Step 4: 提交**

```bash
git add src/test/java/top/enderliquid/audioflow/controller/SessionControllerTest.java
git commit -m "test: 添加密码错误场景测试"
```

---

### Task 9: 测试DELETE /api/sessions/current注销成功

**Files:**
- Modify: `src/test/java/top/enderliquid/audioflow/controller/SessionControllerTest.java`

**Step 1: 添加注销成功测试方法**

```java
@Test
void shouldLogoutSuccessfullyWhenLoggedIn() throws Exception {
    User user = testDataHelper.createTestUser();
    mockLogin(user.getId());

    mockMvc.perform(delete("/api/sessions/current"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.message").value("注销成功"));
}
```

**Step 2: 运行测试**

```bash
./mvnw test -Dtest=SessionControllerTest#shouldLogoutSuccessfullyWhenLoggedIn
```

**Expected:** PASS

**Step 3: 提交**

```bash
git add src/test/java/top/enderliquid/audioflow/controller/SessionControllerTest.java
git commit -m "test: 添加注销成功测试用例"
```

---

### Task 10: 测试DELETE /api/sessions/current未登录

**Files:**
- Modify: `src/test/java/top/enderliquid/audioflow/controller/SessionControllerTest.java`

**Step 1: 添加未登录注销测试方法**

```java
@Test
void shouldReturnErrorWhenLogoutWithoutLogin() throws Exception {
    mockMvc.perform(delete("/api/sessions/current"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(非用户错误码))
            .andExpect(jsonPath("$.message").value(包含"未登录"或"会话"));
}
```

**Step 2: 运行测试**

```bash
./mvnw test -Dtest=SessionControllerTest#shouldReturnErrorWhenLogoutWithoutLogin
```

**Expected:** PASS

**Step 3: 提交**

```bash
git add src/test/java/top/enderliquid/audioflow/controller/SessionControllerTest.java
git commit -m "test: 添加未登录注销测试用例"
```

---

## Phase 3: UserController测试实现

### Task 11: 创建UserControllerTest基础结构

**Files:**
- Create: `src/test/java/top/enderliquid/audioflow/controller/UserControllerTest.java`

**Step 1: 编写UserControllerTest类结构**

```java
package top.enderliquid.audioflow.controller;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import top.enderliquid.audioflow.common.TestDataHelper;
import top.enderliquid.audioflow.config.BaseControllerTest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class UserControllerTest extends BaseControllerTest {

    @Autowired
    private TestDataHelper testDataHelper;

    @BeforeEach
    void setUp() {
        testDataHelper.cleanDatabase();
    }

    // 测试方法将在后续任务中添加
}
```

**Step 2: 验证编译**

```bash
./mvnw test-compile
```

**Expected:** BUILD SUCCESS

**Step 3: 提交**

```bash
git add src/test/java/top/enderliquid/audioflow/controller/UserControllerTest.java
git commit -m "test: 创建UserControllerTest类结构"
```

---

### Task 12: 测试POST /api/users注册成功

**Files:**
- Modify: `src/test/java/top/enderliquid/audioflow/controller/UserControllerTest.java`

**Step 1: 查看UserSaveDTO结构**

```bash
find src/main/java -name "UserSaveDTO.java"
cat $(find src/main/java -name "UserSaveDTO.java")
```

假设有nickname、email、password字段。

**Step 2: 添加注册成功测试方法**

```java
@Test
void shouldRegisterSuccessfullyWhenEmailNotExists() throws Exception {
    String nickname = "new_user";
    String email = "new_user@example.com";
    String password = "new_password_123";

    String requestJson = String.format("{\"nickname\":\"%s\",\"email\":\"%s\",\"password\":\"%s\"}", nickname, email, password);

    mockMvc.perform(post("/api/users")
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestJson))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.email").value(email))
            .andExpect(jsonPath("$.message").value("注册成功"));
}
```

**Step 3: 运行测试**

```bash
./mvnw test -Dtest=UserControllerTest#shouldRegisterSuccessfullyWhenEmailNotExists
```

**Expected:** PASS

**Step 4: 提交**

```bash
git add src/test/java/top/enderliquid/audioflow/controller/UserControllerTest.java
git commit -m "test: 添加注册成功测试用例"
```

---

### Task 13: 测试POST /api/users邮箱已存在

**Files:**
- Modify: `src/test/java/top/enderliquid/audioflow/controller/UserControllerTest.java`

**Step 1: 添加邮箱已存在测试方法**

```java
@Test
void shouldReturnErrorWhenEmailAlreadyExists() throws Exception {
    testDataHelper.createTestUser();

    String email = "test_user@example.com";
    String requestJson = String.format("{\"nickname\":\"another_user\",\"email\":\"%s\",\"password\":\"password\"}", email);

    mockMvc.perform(post("/api/users")
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestJson))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(非用户错误码))
            .andExpect(jsonPath("$.message").value(包含"邮箱"或"已存在"));
}
```

**Step 2: 运行测试**

```bash
./mvnw test -Dtest=UserControllerTest#shouldReturnErrorWhenEmailAlreadyExists
```

**Expected:** PASS

**Step 3: 提交**

```bash
git add src/test/java/top/enderliquid/audioflow/controller/UserControllerTest.java
git commit -m "test: 添加邮箱已存在测试用例"
```

---

### Task 14: 测试POST /api/users参数验证失败

**Files:**
- Modify: `src/test/java/top/enderliquid/audioflow/controller/UserControllerTest.java`

**Step 1: 添加参数验证测试方法**

```java
@Test
void shouldReturnErrorWhenEmailInvalid() throws Exception {
    String requestJson = String.format("{\"nickname\":\"user\",\"email\":\"invalid-email\",\"password\":\"password\"}");

    mockMvc.perform(post("/api/users")
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestJson))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(参数错误码))
            .andExpect(jsonPath("$.message").value(包含"邮箱"或"格式"));
}
```

**Step 2: 运行测试**

```bash
./mvnw test -Dtest=UserControllerTest#shouldReturnErrorWhenEmailInvalid
```

**Expected:** PASS

**Step 3: 提交**

```bash
git add src/test/java/top/enderliquid/audioflow/controller/UserControllerTest.java
git commit -m "test: 添加参数验证测试用例"
```

---

### Task 15: 测试GET /api/users/me获取用户信息成功

**Files:**
- Modify: `src/test/java/top/enderliquid/audioflow/controller/UserControllerTest.java`

**Step 1: 添加获取用户信息测试方法**

```java
@Test
void shouldReturnUserInfoWhenLoggedIn() throws Exception {
    User user = testDataHelper.createTestUser();
    mockLogin(user.getId());

    mockMvc.perform(get("/api/users/me"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.id").value(user.getId()))
            .andExpect(jsonPath("$.data.email").value(user.getEmail()));
}
```

**Step 2: 运行测试**

```bash
./mvnw test -Dtest=UserControllerTest#shouldReturnUserInfoWhenLoggedIn
```

**Expected:** PASS

**Step 3: 提交**

```bash
git add src/test/java/top/enderliquid/audioflow/controller/UserControllerTest.java
git commit -m "test: 添加获取用户信息测试用例"
```

---

### Task 16: 测试GET /api/users/me未登录

**Files:**
- Modify: `src/test/java/top/enderliquid/audioflow/controller/UserControllerTest.java`

**Step 1: 添加未登录测试方法**

```java
@Test
void shouldReturnErrorWhenGetUserWithoutLogin() throws Exception {
    mockMvc.perform(get("/api/users/me"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(非用户错误码))
            .andExpect(jsonPath("$.message").value(包含"未登录"或"会话"));
}
```

**Step 2: 运行测试**

```bash
./mvnw test -Dtest=UserControllerTest#shouldReturnErrorWhenGetUserWithoutLogin
```

**Expected:** PASS

**Step 3: 提交**

```bash
git add src/test/java/top/enderliquid/audioflow/controller/UserControllerTest.java
git commit -m "test: 添加未登录获取用户信息测试用例"
```

---

### Task 17: 测试PATCH /api/users/me/password修改密码成功

**Files:**
- Modify: `src/test/java/top/enderliquid/audioflow/controller/UserControllerTest.java`

**Step 1: 查看UserUpdatePasswordDTO结构**

```bash
find src/main/java -name "UserUpdatePasswordDTO.java"
cat $(find src/main/java -name "UserUpdatePasswordDTO.java")
```

假设有oldPassword和newPassword字段。

**Step 2: 添加修改密码成功测试方法**

```java
@Test
void shouldUpdatePasswordSuccessfullyWhenCorrectOldPassword() throws Exception {
    User user = testDataHelper.createTestUser();
    mockLogin(user.getId());

    String oldPassword = "test_password_123";
    String newPassword = "new_password_456";

    String requestJson = String.format("{\"oldPassword\":\"%s\",\"newPassword\":\"%s\"}", oldPassword, newPassword);

    mockMvc.perform(patch("/api/users/me/password")
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestJson))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.message").value("密码修改成功，请重新登录"));
}
```

**Step 3: 运行测试**

```bash
./mvnw test -Dtest=UserControllerTest#shouldUpdatePasswordSuccessfullyWhenCorrectOldPassword
```

**Expected:** PASS

**Step 4: 提交**

```bash
git add src/test/java/top/enderliquid/audioflow/controller/UserControllerTest.java
git commit -m "test: 添加修改密码成功测试用例"
```

---

### Task 18: 测试PATCH /api/users/me/password旧密码错误

**Files:**
- Modify: `src/test/java/top/enderliquid/audioflow/controller/UserControllerTest.java`

**Step 1: 添加旧密码错误测试方法**

```java
@Test
void shouldReturnErrorWhenOldPasswordIncorrect() throws Exception {
    User user = testDataHelper.createTestUser();
    mockLogin(user.getId());

    String wrongOldPassword = "wrong_password";
    String newPassword = "new_password_456";

    String requestJson = String.format("{\"oldPassword\":\"%s\",\"newPassword\":\"%s\"}", wrongOldPassword, newPassword);

    mockMvc.perform(patch("/api/users/me/password")
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestJson))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(非用户错误码))
            .andExpect(jsonPath("$.message").value(包含"密码"或"错误"));
}
```

**Step 2: 运行测试**

```bash
./mvnw test -Dtest=UserControllerTest#shouldReturnErrorWhenOldPasswordIncorrect
```

**Expected:** PASS

**Step 3: 提交**

```bash
git add src/test/java/top/enderliquid/audioflow/controller/UserControllerTest.java
git commit -m "test: 添加旧密码错误测试用例"
```

---

### Task 19: 测试PATCH /api/users/me/password未登录

**Files:**
- Modify: `src/test/java/top/enderliquid/audioflow/controller/UserControllerTest.java`

**Step 1: 添加未登录测试方法**

```java
@Test
void shouldReturnErrorWhenUpdatePasswordWithoutLogin() throws Exception {
    String requestJson = String.format("{\"oldPassword\":\"old\",\"newPassword\":\"new\"}");

    mockMvc.perform(patch("/api/users/me/password")
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestJson))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(非用户错误码))
            .andExpect(jsonPath("$.message").value(包含"未登录"或"会话"));
}
```

**Step 2: 运行测试**

```bash
./mvnw test -Dtest=UserControllerTest#shouldReturnErrorWhenUpdatePasswordWithoutLogin
```

**Expected:** PASS

**Step 3: 提交**

```bash
git add src/test/java/top/enderliquid/audioflow/controller/UserControllerTest.java
git commit -m "test: 添加未登录修改密码测试用例"
```

---

## Phase 4: SongController基础CRUD测试

### Task 20: 创建SongControllerTest基础结构

**Files:**
- Create: `src/test/java/top/enderliquid/audioflow/controller/SongControllerTest.java`

**Step 1: 编写SongControllerTest类结构**

```java
package top.enderliquid.audioflow.controller;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import top.enderliquid.audioflow.common.TestDataHelper;
import top.enderliquid.audioflow.config.BaseControllerTest;

import java.nio.file.Files;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class SongControllerTest extends BaseControllerTest {

    @Autowired
    private TestDataHelper testDataHelper;

    private User testUser;
    private Song testSong;

    @BeforeEach
    void setUp() {
        testDataHelper.cleanDatabase();
        testUser = testDataHelper.createTestUser();
        testSong = testDataHelper.createTestSong(testUser.getId());
    }

    // 测试方法将在后续任务中添加
}
```

**Step 2: 验证编译**

```bash
./mvnw test-compile
```

**Expected:** BUILD SUCCESS

**Step 3: 提交**

```bash
git add src/test/java/top/enderliquid/audioflow/controller/SongControllerTest.java
git commit -m "test: 创建SongControllerTest类结构"
```

---

### Task 21: 测试GET /api/songs分页查询成功

**Files:**
- Modify: `src/test/java/top/enderliquid/audioflow/controller/SongControllerTest.java`

**Step 1: 查看SongPageDTO结构**

```bash
find src/main/java -name "SongPageDTO.java"
cat $(find src/main/java -name "SongPageDTO.java")
```

**Step 2: 添加分页查询测试方法**

```java
@Test
void shouldReturnSongPageWhenQueryWithPagination() throws Exception {
    mockMvc.perform(get("/api/songs")
            .param("page", "1")
            .param("size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.records").isArray());
}
```

**Step 3: 运行测试**

```bash
./mvnw test -Dtest=SongControllerTest#shouldReturnSongPageWhenQueryWithPagination
```

**Expected:** PASS

**Step 4: 提交**

```bash
git add src/test/java/top/enderliquid/audioflow/controller/SongControllerTest.java
git commit -m "test: 添加分页查询测试用例"
```

---

### Task 22: 测试GET /api/songs空列表

**Files:**
- Modify: `src/test/java/top/enderliquid/audioflow/controller/SongControllerTest.java`

**Step 1: 添加空列表测试方法**

```java
@Test
void shouldReturnEmptyListWhenNoSongs() throws Exception {
    testDataHelper.cleanDatabase();

    mockMvc.perform(get("/api/songs")
            .param("page", "1")
            .param("size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.records").isEmpty());
}
```

**Step 2: 运行测试**

```bash
./mvnw test -Dtest=SongControllerTest#shouldReturnEmptyListWhenNoSongs
```

**Expected:** PASS

**Step 3: 提交**

```bash
git add src/test/java/top/enderliquid/audioflow/controller/SongControllerTest.java
git commit -m "test: 添加空列表测试用例"
```

---

### Task 23: 测试GET /api/songs/{id}获取歌曲成功

**Files:**
- Modify: `src/test/java/top/enderliquid/audioflow/controller/SongControllerTest.java`

**Step 1: 添加获取歌曲测试方法**

```java
@Test
void shouldReturnSongInfoWhenSongExists() throws Exception {
    mockMvc.perform(get("/api/songs/{id}", testSong.getId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.id").value(testSong.getId()))
            .andExpect(jsonPath("$.data.title").value(testSong.getTitle()));
}
```

**Step 2: 运行测试**

```bash
./mvnw test -Dtest=SongControllerTest#shouldReturnSongInfoWhenSongExists
```

**Expected:** PASS

**Step 3: 提交**

```bash
git add src/test/java/top/enderliquid/audioflow/controller/SongControllerTest.java
git commit -m "test: 添加获取歌曲信息测试用例"
```

---

### Task 24: 测试GET /api/songs/{id}歌曲不存在

**Files:**
- Modify: `src/test/java/top/enderliquid/audioflow/controller/SongControllerTest.java`

**Step 1: 添加歌曲不存在测试方法**

```java
@Test
void shouldReturnErrorWhenSongNotExists() throws Exception {
    Long nonExistentId = 999999L;

    mockMvc.perform(get("/api/songs/{id}", nonExistentId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(非用户错误码))
            .andExpect(jsonPath("$.message").value(包含"不存在"或"歌曲"));
}
```

**Step 2: 运行测试**

```bash
./mvnw test -Dtest=SongControllerTest#shouldReturnErrorWhenSongNotExists
```

**Expected:** PASS

**Step 3: 提交**

```bash
git add src/test/java/top/enderliquid/audioflow/controller/SongControllerTest.java
git commit -m "test: 添加歌曲不存在测试用例"
```

---

### Task 25: 测试GET /api/songs/{id}/play获取播放URL

**Files:**
- Modify: `src/test/java/top/enderliquid/audioflow/controller/SongControllerTest.java`

**Step 1: 添加获取播放URL测试方法**

```java
@Test
void shouldRedirectWhenGetSongPlayUrl() throws Exception {
    mockMvc.perform(get("/api/songs/{id}/play", testSong.getId()))
            .andExpect(status().is3xxRedirection())
            .andExpect(header().exists("Location"));
}
```

**Step 2: 运行测试**

```bash
./mvnw test -Dtest=SongControllerTest#shouldRedirectWhenGetSongPlayUrl
```

**Expected:** PASS

**Step 3: 提交**

```bash
git add src/test/java/top/enderliquid/audioflow/controller/SongControllerTest.java
git commit -m "test: 添加获取播放URL测试用例"
```

---

### Task 26: 测试GET /api/songs/{id}/play歌曲不存在

**Files:**
- Modify: `src/test/java/top/enderliquid/audioflow/controller/SongControllerTest.java`

**Step 1: 添加歌曲不存在测试方法**

```java
@Test
void shouldReturn404WhenSongNotExistsPlayUrl() throws Exception {
    Long nonExistentId = 999999L;

    mockMvc.perform(get("/api/songs/{id}/play", nonExistentId))
            .andExpect(status().isNotFound());
}
```

**Step 2: 运行测试**

```bash
./mvnw test -Dtest=SongControllerTest#shouldReturn404WhenSongNotExistsPlayUrl
```

**Expected:** PASS

**Step 3: 提交**

```bash
git add src/test/java/top/enderliquid/audioflow/controller/SongControllerTest.java
git commit -m "test: 添加播放URL不存在测试用例"
```

---

### Task 27: 测试DELETE /api/songs/{id}删除成功

**Files:**
- Modify: `src/test/java/top/enderliquid/audioflow/controller/SongControllerTest.java`

**Step 1: 添加删除成功测试方法**

```java
@Test
void shouldDeleteSuccessfullyWhenOwner() throws Exception {
    mockLogin(testUser.getId());

    mockMvc.perform(delete("/api/songs/{id}", testSong.getId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.message").value("删除成功"));
}
```

**Step 2: 运行测试**

```bash
./mvnw test -Dtest=SongControllerTest#shouldDeleteSuccessfullyWhenOwner
```

**Expected:** PASS

**Step 3: 提交**

```bash
git add src/test/java/top/enderliquid/audioflow/controller/SongControllerTest.java
git commit -m "test: 添加删除成功测试用例"
```

---

### Task 28: 测试DELETE /api/songs/{id}未登录

**Files:**
- Modify: `src/test/java/top/enderliquid/audioflow/controller/SongControllerTest.java`

**Step 1: 添加未登录测试方法**

```java
@Test
void shouldReturnErrorWhenDeleteWithoutLogin() throws Exception {
    mockMvc.perform(delete("/api/songs/{id}", testSong.getId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(非用户错误码))
            .andExpect(jsonPath("$.message").value(包含"未登录"或"会话"));
}
```

**Step 2: 运行测试**

```bash
./mvnw test -Dtest=SongControllerTest#shouldReturnErrorWhenDeleteWithoutLogin
```

**Expected:** PASS

**Step 3: 提交**

```bash
git add src/test/java/top/enderliquid/audioflow/controller/SongControllerTest.java
git commit -m "test: 添加未登录删除测试用例"
```

---

### Task 29: 测试DELETE /api/songs/{id}无权限（非所有者）

**Files:**
- Modify: `src/test/java/top/enderliquid/audioflow/controller/SongControllerTest.java`

**Step 1: 添加无权限测试方法**

```java
@Test
void shouldReturnErrorWhenDeleteByNonOwner() throws Exception {
    User anotherUser = testDataHelper.createTestUser();
    mockLogin(anotherUser.getId());

    mockMvc.perform(delete("/api/songs/{id}", testSong.getId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(非用户错误码))
            .andExpect(jsonPath("$.message").value(包含"权限"或"所有者"));
}
```

**Step 2: 运行测试**

```bash
./mvnw test -Dtest=SongControllerTest#shouldReturnErrorWhenDeleteByNonOwner
```

**Expected:** PASS

**Step 3: 提交**

```bash
git add src/test/java/top/enderliquid/audioflow/controller/SongControllerTest.java
git commit -m "test: 添加删除无权限测试用例"
```

---

### Task 30: 测试POST /api/songs上传歌曲成功

**Files:**
- Modify: `src/test/java/top/enderliquid/audioflow/controller/SongControllerTest.java`

**Step 1: 查看SongSaveDTO结构**

```bash
find src/main/java -name "SongSaveDTO.java"
cat $(find src/main/java -name "SongSaveDTO.java")
```

**Step 2: 添加上传成功测试方法**

需要使用用户提供的测试音频文件：

```java
@Test
void shouldUploadSuccessfullyWhenLoggedIn() throws Exception {
    mockLogin(testUser.getId());

    Resource audioResource = new ClassPathResource("audio/test-song.mp3");
    File audioFile = audioResource.getFile();

    MockMultipartFile file = new MockMultipartFile(
        "file",
        "test-song.mp3",
        "audio/mpeg",
        Files.readAllBytes(audioFile.toPath())
    );

    mockMvc.perform(multipart("/api/songs")
            .file(file)
            .param("title", "New Song")
            .param("artist", "New Artist")
            .param("album", "New Album"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.message").value("上传成功"));
}
```

**Step 3: 运行测试**

```bash
./mvnw test -Dtest=SongControllerTest#shouldUploadSuccessfullyWhenLoggedIn
```

**Expected:** PASS

**Step 4: 提交**

```bash
git add src/test/java/top/enderliquid/audioflow/controller/SongControllerTest.java
git commit -m "test: 添加上传成功测试用例"
```

---

### Task 31: 测试POST /api/songs未登录

**Files:**
- Modify: `src/test/java/top/enderliquid/audioflow/controller/SongControllerTest.java`

**Step 1: 添加未登录测试方法**

```java
@Test
void shouldReturnErrorWhenUploadWithoutLogin() throws Exception {
    Resource audioResource = new ClassPathResource("audio/test-song.mp3");
    File audioFile = audioResource.getFile();

    MockMultipartFile file = new MockMultipartFile(
        "file",
        "test-song.mp3",
        "audio/mpeg",
        Files.readAllBytes(audioFile.toPath())
    );

    mockMvc.perform(multipart("/api/songs")
            .file(file)
            .param("title", "New Song")
            .param("artist", "New Artist"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(非用户错误码))
            .andExpect(jsonPath("$.message").value(包含"未登录"或"会话"));
}
```

**Step 2: 运行测试**

```bash
./mvnw test -Dtest=SongControllerTest#shouldReturnErrorWhenUploadWithoutLogin
```

**Expected:** PASS

**Step 3: 提交**

```bash
git add src/test/java/top/enderliquid/audioflow/controller/SongControllerTest.java
git commit -m "test: 添加未登录上传测试用例"
```

---

## Phase 5: SongController更新和权限测试

### Task 32: 测试PATCH /api/songs/{id}更新成功

**Files:**
- Modify: `src/test/java/top/enderliquid/audioflow/controller/SongControllerTest.java`

**Step 1: 查看SongUpdateDTO结构**

```bash
find src/main/java -name "SongUpdateDTO.java"
cat $(find src/main/java -name "SongUpdateDTO.java")
```

**Step 2: 添加更新成功测试方法**

```java
@Test
void shouldUpdateSuccessfullyWhenOwner() throws Exception {
    mockLogin(testUser.getId());

    String requestJson = String.format("{\"title\":\"Updated Title\",\"artist\":\"Updated Artist\"}");

    mockMvc.perform(patch("/api/songs/{id}", testSong.getId())
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestJson))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.title").value("Updated Title"));
}
```

**Step 3: 运行测试**

```bash
./mvnw test -Dtest=SongControllerTest#shouldUpdateSuccessfullyWhenOwner
```

**Expected:** PASS

**Step 4: 提交**

```bash
git add src/test/java/top/enderliquid/audioflow/controller/SongControllerTest.java
git commit -m "test: 添加更新成功测试用例"
```

---

### Task 33: 测试PATCH /api/songs/{id}未登录

**Files:**
- Modify: `src/test/java/top/enderliquid/audioflow/controller/SongControllerTest.java`

**Step 1: 添加未登录测试方法**

```java
@Test
void shouldReturnErrorWhenUpdateWithoutLogin() throws Exception {
    String requestJson = String.format("{\"title\":\"Updated Title\"}");

    mockMvc.perform(patch("/api/songs/{id}", testSong.getId())
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestJson))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(非用户错误码))
            .andExpect(jsonPath("$.message").value(包含"未登录"或"会话"));
}
```

**Step 2: 运行测试**

```bash
./mvnw test -Dtest=SongControllerTest#shouldReturnErrorWhenUpdateWithoutLogin
```

**Expected:** PASS

**Step 3: 提交**

```bash
git add src/test/java/top/enderliquid/audioflow/controller/SongControllerTest.java
git commit -m "test: 添加未登录更新测试用例"
```

---

### Task 34: 测试PATCH /api/songs/{id}无权限

**Files:**
- Modify: `src/test/java/top/enderliquid/audioflow/controller/SongControllerTest.java`

**Step 1: 添加无权限测试方法**

```java
@Test
void shouldReturnErrorWhenUpdateByNonOwner() throws Exception {
    User anotherUser = testDataHelper.createTestUser();
    mockLogin(anotherUser.getId());

    String requestJson = String.format("{\"title\":\"Updated Title\"}");

    mockMvc.perform(patch("/api/songs/{id}", testSong.getId())
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestJson))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(非用户错误码))
            .andExpect(jsonPath("$.message").value(包含"权限"或"所有者"));
}
```

**Step 2: 运行测试**

```bash
./mvnw test -Dtest=SongControllerTest#shouldReturnErrorWhenUpdateByNonOwner
```

**Expected:** PASS

**Step 3: 提交**

```bash
git add src/test/java/top/enderliquid/audioflow/controller/SongControllerTest.java
git commit -m "test: 添加更新无权限测试用例"
```

---

### Task 35: 测试DELETE /api/songs/{id}/force管理员删除

**Files:**
- Modify: `src/test/java/top/enderliquid/audioflow/controller/SongControllerTest.java`

**Step 1: 添加管理员强制删除测试方法**

```java
@Test
void shouldForceDeleteSuccessfullyWhenAdmin() throws Exception {
    mockAdminLogin(testUser.getId());

    mockMvc.perform(delete("/api/songs/{id}/force", testSong.getId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.message").value("管理员强制删除成功"));
}
```

**Step 2: 运行测试**

```bash
./mvnw test -Dtest=SongControllerTest#shouldForceDeleteSuccessfullyWhenAdmin
```

**Expected:** PASS

**Step 3: 提交**

```bash
git add src/test/java/top/enderliquid/audioflow/controller/SongControllerTest.java
git commit -m "test: 添加管理员删除测试用例"
```

---

### Task 36: 测试DELETE /api/songs/{id}/force非管理员

**Files:**
- Modify: `src/test/java/top/enderliquid/audioflow/controller/SongControllerTest.java`

**Step 1: 添加非管理员测试方法**

```java
@Test
void shouldReturnErrorWhenForceDeleteByNonAdmin() throws Exception {
    mockLogin(testUser.getId());

    mockMvc.perform(delete("/api/songs/{id}/force", testSong.getId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(非用户错误码))
            .andExpect(jsonPath("$.message").value(包含"权限"或"管理员"));
}
```

**Step 2: 运行测试**

```bash
./mvnw test -Dtest=SongControllerTest#shouldReturnErrorWhenForceDeleteByNonAdmin
```

**Expected:** PASS

**Step 3: 提交**

```bash
git add src/test/java/top/enderliquid/audioflow/controller/SongControllerTest.java
git commit -m "test: 添加非管理员删除测试用例"
```

---

### Task 37: 测试PATCH /api/songs/{id}/force管理员更新

**Files:**
- Modify: `src/test/java/top/enderliquid/audioflow/controller/SongControllerTest.java`

**Step 1: 添加管理员强制更新测试方法**

```java
@Test
void shouldForceUpdateSuccessfullyWhenAdmin() throws Exception {
    mockAdminLogin(testUser.getId());

    String requestJson = String.format("{\"title\":\"Force Updated Title\"}");

    mockMvc.perform(patch("/api/songs/{id}/force", testSong.getId())
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestJson))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.title").value("Force Updated Title"));
}
```

**Step 2: 运行测试**

```bash
./mvnw test -Dtest=SongControllerTest#shouldForceUpdateSuccessfullyWhenAdmin
```

**Expected:** PASS

**Step 3: 提交**

```bash
git add src/test/java/top/enderliquid/audioflow/controller/SongControllerTest.java
git commit -m "test: 添加管理员更新测试用例"
```

---

### Task 38: 测试PATCH /api/songs/{id}/force非管理员

**Files:**
- Modify: `src/test/java/top/enderliquid/audioflow/controller/SongControllerTest.java`

**Step 1: 添加非管理员测试方法**

```java
@Test
void shouldReturnErrorWhenForceUpdateByNonAdmin() throws Exception {
    mockLogin(testUser.getId());

    String requestJson = String.format("{\"title\":\"Title\"}");

    mockMvc.perform(patch("/api/songs/{id}/force", testSong.getId())
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestJson))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(非用户错误码))
            .andExpect(jsonPath("$.message").value(包含"权限"或"管理员"));
}
```

**Step 2: 运行测试**

```bash
./mvnw test -Dtest=SongControllerTest#shouldReturnErrorWhenForceUpdateByNonAdmin
```

**Expected:** PASS

**Step 3: 提交**

```bash
git add src/test/java/top/enderliquid/audioflow/controller/SongControllerTest.java
git commit -m "test: 添加非管理员更新测试用例"
```

---

## Phase 6: 最终验证和清理

### Task 39: 运行所有测试

**Files:**
- None

**Step 1: 运行全部测试**

```bash
./mvnw test
```

**Expected:** 所有测试通过（BUILD SUCCESS）

**Step 2: 查看测试统计**

输出应该显示类似：
```
Tests run: XX, Failures: 0, Errors: 0, Skipped: 0
```

**Step 3: 提交**

```bash
git add .
git commit -m "test: 完成所有API接口测试覆盖"
```

---

### Task 40: 清理和文档更新

**Files:**
- Update: `README.md` (如果存在)

**Step 1: 检查是否有README**

```bash
ls README.md
```

**Step 2: 如果存在，更新测试部分**

在README中添加测试说明：

```markdown
## 测试

项目使用JUnit 5和Spring Boot Test实现Controller层集成测试。

### 运行测试

```bash
# 运行所有测试
./mvnw test

# 运行特定测试类
./mvnw test -Dtest=UserControllerTest

# 运行特定测试方法
./mvnw test -Dtest=SessionControllerTest#shouldLoginSuccessfullyWhenCredentialsCorrect
```

### 测试覆盖

目前覆盖以下所有API接口：

- **UserController** (3个接口): 注册、获取用户信息、修改密码
- **SongController** (8个接口): 上传、分页查询、删除、强制删除、获取信息、播放URL、更新、强制更新
- **SessionController** (2个接口): 登录、注销

测试包括成功场景、参数验证、权限验证和业务异常场景。
```

**Step 3: 提交**

```bash
git add README.md
git commit -m "docs: 更新README添加测试说明"
```

---

## 完成检查清单

- [ ] Phase 1: 基础设施搭建完成
- [ ] Phase 2: SessionController测试完成（2个接口，4个测试用例）
- [ ] Phase 3: UserController测试完成（3个接口，8个测试用例）
- [ ] Phase 4: SongController基础CRUD测试完成（5个接口，8个测试用例）
- [ ] Phase 5: SongController更新和权限测试完成（3个接口，8个测试用例）
- [ ] Phase 6: 所有测试通过
- [ ] README更新完成

**总计:** 13个API接口，约28个测试用例

## 注意事项

1. **错误码**: 计划中使用了占位符`非用户错误码`和`参数错误码`，实际实现时需要根据项目中的错误码定义进行调整
2. **DTO字段**: 计划中假设了DTO字段名，实际实现时需要查看真实的DTO结构
3. **实体字段**: TestDataHelper中假设了实体字段名，需要根据实际调整
4. **测试文件**: Task 5需要用户提供测试音频文件才能继续后续的上传测试
5. **密码加密**: 实际测试中需要根据UserService的密码加密逻辑调整测试密码

## 回滚计划

如果某个Task失败导致测试环境不稳定：

```bash
git reset --hard HEAD~N  # 回退N个提交
./mvnw test            # 验证当前状态
```

---

## 附录: 快速参考

### 常用MVN命令

```bash
./mvnw test                                    # 运行所有测试
./mvnw test -Dtest=ClassName                   # 运行指定测试类
./mvnw test -Dtest=ClassName#methodName        # 运行指定测试方法
./mvnw clean test                              # 清理后运行测试
./mvnw test-compile                            # 编译测试代码
```

### 测试命名规范

- `should{操作}When{条件}` - 成功场景
- `shouldReturnErrorWhen{条件}` - 异常场景
- `shouldThrow{异常}When{条件}` - 预期抛出异常

### 权限Mock方法

```java
mockLogin(userId);              // 模拟普通用户登录
mockAdminLogin(userId);          // 模拟管理员登录
// 不调用方法 = 未登录状态
```

### Git提交规范

- `test: 某某测试用例` - 添加或修改测试
- `feat: 某某功能` - 新功能
- `fix: 某某问题` - 修复问题
- `docs: 某某文档` - 文档更新
