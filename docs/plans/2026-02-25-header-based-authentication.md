# Header基于的鉴权改造实现计划

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**目标:** 将AudioFlow项目从Cookie模式的Sa-Token鉴权改造为前端手动在请求头中携带Token的无Cookie模式

**架构:** 创建新的LoginResult包装类包含UserVO和SaTokenInfo，修改登录和注册接口返回LoginResult，前端从响应中提取token存储到localStorage，后续请求在header中携带token

**技术栈:** Java 21, Spring Boot 3.5.10, Sa-Token 1.44.0, JavaScript

---

## Task 1: 创建LoginResult包装类

**Files:**
- Create: `src/main/java/top/enderliquid/audioflow/dto/response/LoginResult.java`

**Step 1: 创建LoginResult类**

```java
package top.enderliquid.audioflow.dto.response;

import cn.dev33.satoken.stp.SaTokenInfo;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import top.enderliquid.audioflow.dto.response.user.UserVO;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LoginResult {
    private UserVO user;
    private SaTokenInfo tokenInfo;
}
```

**Step 2: 编译验证**

Run: `./mvnw clean compile`
Expected: 编译成功

**Step 3: 提交**

```bash
git add src/main/java/top/enderliquid/audioflow/dto/response/LoginResult.java
git commit -m "新增LoginResult包装类，用于返回登录和注册的用户信息和token"
```

---

## Task 2: 修改SessionController登录接口

**Files:**
- Modify: `src/main/java/top/enderliquid/audioflow/controller/SessionController.java:34-38`

**Step 1: 编写单元测试验证LoginResult返回**

修改 `src/test/java/top/enderliquid/audioflow/controller/SessionControllerTest.java` 中的登录测试：

```java
void shouldLoginSuccessfully() throws Exception {
    String email = "test@example.com";
    String password = "test_password_123";

    Map<String, String> registerDto = new HashMap<>();
    registerDto.put("email", email);
    registerDto.put("name", "Test User");
    registerDto.put("password", password);
    String registerJson = objectMapper.writeValueAsString(registerDto);

    mockMvc.perform(post("/api/users")
            .contentType("application/json")
            .content(registerJson))
            .andExpect(status().isOk());

    Map<String, String> loginDto = new HashMap<>();
    loginDto.put("email", email);
    loginDto.put("password", password);
    String loginJson = objectMapper.writeValueAsString(loginDto);

    MvcResult result = mockMvc.perform(post("/api/sessions")
            .contentType("application/json")
            .content(loginJson))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.user.email").value(email))
            .andExpect(jsonPath("$.data.user.name").value("Test User"))
            .andExpect(jsonPath("$.data.tokenInfo").isMap())
            .andExpect(jsonPath("$.data.tokenInfo.tokenName").isNotEmpty())
            .andExpect(jsonPath("$.data.tokenInfo.tokenValue").isNotEmpty())
            .andReturn();
}
```

**Step 2: 运行测试验证失败**

Run: `./mvnw test -Dtest=SessionControllerTest#shouldLoginSuccessfully`
Expected: FAIL - 测试失败因为LoginResult未返回

**Step 3: 修改SessionController.login方法**

```java


@PostMapping
@RateLimit(
        refillRate = "3/60",
        capacity = 3,
        limitType = LimitType.IP,
        message = "登录尝试过于频繁，请稍后再试"
)
public HttpResponseBody<top.enderliquid.audioflow.dto.response.session.LoginResultVO> login(@Valid @RequestBody UserVerifyPasswordDTO dto) {
    UserVO userVO = userService.verifyUserPassword(dto);
    StpUtil.login(userVO.getId());
    SaTokenInfo tokenInfo = StpUtil.getTokenInfo();
    return HttpResponseBody.ok(new top.enderliquid.audioflow.dto.response.session.LoginResultVO(userVO, tokenInfo), "登录成功");
}
```

**Step 4: 运行测试验证通过**

Run: `./mvnw test -Dtest=SessionControllerTest#shouldLoginSuccessfully`
Expected: PASS - 测试通过

**Step 5: 提交**

```bash
git add src/main/java/top/enderliquid/audioflow/controller/SessionController.java src/test/java/top/enderliquid/audioflow/controller/SessionControllerTest.java
git commit -m "修改登录接口返回LoginResult，包含用户信息和token"
```

---

## Task 3: 修改UserController注册接口

**Files:**
- Modify: `src/main/java/top/enderliquid/audioflow/controller/UserController.java:35-39`
- Modify: `src/test/java/top/enderliquid/audioflow/controller/UserControllerTest.java`

**Step 1: 编写单元测试验证LoginResult返回**

修改 `src/test/java/top/enderliquid/audioflow/controller/UserControllerTest.java` 中的注册测试：

```java
void shouldRegisterSuccessfully() throws Exception {
    String email = "newuser@example.com";
    String password = "new_password_123";

    Map<String, String> registerDto = new HashMap<>();
    registerDto.put("email", email);
    registerDto.put("name", "New User");
    registerDto.put("password", password);
    String registerJson = objectMapper.writeValueAsString(registerDto);

    mockMvc.perform(post("/api/users")
            .contentType("application/json")
            .content(registerJson))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.user.email").value(email))
            .andExpect(jsonPath("$.data.user.name").value("New User"))
            .andExpect(jsonPath("$.data.tokenInfo").isMap())
            .andExpect(jsonPath("$.data.tokenInfo.tokenName").isNotEmpty())
            .andExpect(jsonPath("$.data.tokenInfo.tokenValue").isNotEmpty());
}
```

**Step 2: 运行测试验证失败**

Run: `./mvnw test -Dtest=UserControllerTest#shouldRegisterSuccessfully`
Expected: FAIL - 测试失败因为LoginResult未返回

**Step 3: 修改UserController.register方法**

```java
import cn.dev33.satoken.stp.SaTokenInfo;
import top.enderliquid.audioflow.dto.response.session.LoginResultVO;

@PostMapping
@RateLimit(
        refillRate = "3/60",
        capacity = 3,
        limitType = LimitType.IP,
        message = "注册过于频繁，请稍后再试"
)
public HttpResponseBody<LoginResultVO> register(@Valid @RequestBody UserSaveDTO dto) {
    UserVO userVO = userService.saveUser(dto);
    StpUtil.login(userVO.getId());
    SaTokenInfo tokenInfo = StpUtil.getTokenInfo();
    return HttpResponseBody.ok(new LoginResultVO(userVO, tokenInfo), "注册成功");
}
```

**Step 4: 运行测试验证通过**

Run: `./mvnw test -Dtest=UserControllerTest#shouldRegisterSuccessfully`
Expected: PASS - 测试通过

**Step 5: 提交**

```bash
git add src/main/java/top/enderliquid/audioflow/controller/UserController.java src/test/java/top/enderliquid/audioflow/controller/UserControllerTest.java
git commit -m "修改注册接口返回LoginResult，包含用户信息和token"
```

---

## Task 4: 禁用Cookie读取配置

**Files:**
- Modify: `src/main/resources/application.properties:64-78`

**Step 1: 添加禁用Cookie读取配置**

在Sa-Token配置部分添加：

```properties
# 是否从cookie中读取token
sa-token.is-read-cookie=false
```

**Step 2: 编译验证**

Run: `./mvnw clean compile`
Expected: 编译成功

**Step 3: 提交**

```bash
git add src/main/resources/application.properties
git commit -m "禁用Sa-Token从Cookie读取token，改为仅从header读取"
```

---

## Task 5: 修改upload-test.html登录函数

**Files:**
- Modify: `upload-test.html:206-234`

**Step 1: 修改登录函数，存储token到localStorage**

```javascript
function login() {
    const baseUrl = document.getElementById('baseUrl').value;
    const email = document.getElementById('email').value;
    const password = document.getElementById('password').value;
    const status = document.getElementById('loginStatus');

    if (!email || !password) {
        status.innerHTML = '请填写邮箱和密码';
        status.className = 'status failure';
        return;
    }

    status.innerHTML = '登录中...';
    status.className = 'status';

    fetch(`${baseUrl}/api/sessions`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify({
            email: email,
            password: password
        })
    }).then(response => {
        return response.json();
    }).then(data => {
        if (data.success) {
            localStorage.setItem('tokenName', data.data.tokenInfo.tokenName);
            localStorage.setItem('tokenValue', data.data.tokenInfo.tokenValue);
            status.innerHTML = `登录成功 - ${data.data.user.name}`;
            status.className = 'status success';
        } else {
            status.innerHTML = `登录失败: ${data.message}`;
            status.className = 'status failure';
        }
    }).catch(error => {
        status.innerHTML = `登录失败: ${error.message}`;
        status.className = 'status failure';
    });
}
```

**Step 2: 提交**

```bash
git add upload-test.html
git commit -m "修改登录函数，将token存储到localStorage"
```

---

## Task 6: 添加token携带辅助函数

**Files:**
- Modify: `upload-test.html`（在script标签内添加新函数，约第258行之后）

**Step 1: 添加getAuthHeaders函数**

```javascript
function getAuthHeaders() {
    const headers = {
        'Content-Type': 'application/json'
    };

    const tokenName = localStorage.getItem('tokenName');
    const tokenValue = localStorage.getItem('tokenValue');

    if (tokenName && tokenValue) {
        headers[tokenName] = tokenValue;
    }

    return headers;
}
```

**Step 2: 提交**

```bash
git add upload-test.html
git commit -m "添加getAuthHeaders辅助函数，用于获取携带token的headers"
```

---

## Task 7: 修改prepareUpload函数

**Files:**
- Modify: `upload-test.html:269-300`

**Step 1: 修改prepareUpload使用getAuthHeaders**

```javascript
async function prepareUpload() {
    const baseUrl = document.getElementById('baseUrl').value;
    const songName = document.getElementById('songName').value;
    const description = document.getElementById('description').value;
    const fileType = document.getElementById('fileType').value;

    updateStep('step2', 'in-progress', '2. 准备上传中...');

    const response = await fetch(`${baseUrl}/api/songs/prepare`, {
        method: 'POST',
        headers: getAuthHeaders(),
        body: JSON.stringify({
            mimeType: fileType,
            name: songName || '',
            description: description || null
        })
    });

    console.log('Prepare Response Status:', response.status);
    const data = await response.json();
    console.log('Prepare Response Data:', data);

    if (!data.success) {
        throw new Error(data.message || '准备上传失败');
    }

    updateStep('step2', 'success', '2. 准备上传成功');
    return data.data;
}
```

**Step 2: 提交**

```bash
git add upload-test.html
git commit -m "修改prepareUpload函数使用header携带token"
```

---

## Task 8: 修改completeUpload函数

**Files:**
- Modify: `upload-test.html:321-345`

**Step 1: 修改completeUpload使用getAuthHeaders**

```javascript
async function completeUpload(songId) {
    const baseUrl = document.getElementById('baseUrl').value;

    updateStep('step4', 'in-progress', '4. 完成验证中...');

    const response = await fetch(`${baseUrl}/api/songs/complete`, {
        method: 'POST',
        headers: getAuthHeaders(),
        body: JSON.stringify({
            songId: songId
        })
    });

    const data = await response.json();

    if (!data.success) {
        throw new Error(data.message || '完成上传失败');
    }

    updateStep('step4', 'success', '4. 完成验证成功');
    return data.data;
}
```

**Step 2: 提交**

```bash
git add upload-test.html
git commit -m "修改completeUpload函数使用header携带token"
```

---

## Task 9: 添加401错误处理

**Files:**
- Modify: `upload-test.html`（在script标签内添加辅助函数）

**Step 1: 添加401错误处理函数**

```javascript
async function fetchWithAuth(url, options = {}) {
    options.headers = options.headers || {};
    Object.assign(options.headers, getAuthHeaders());

    const response = await fetch(url, options);

    if (response.status === 401) {
        localStorage.removeItem('tokenName');
        localStorage.removeItem('tokenValue');
        alert('登录已过期，请重新登录');
        throw new Error('未登录');
    }

    return response;
}
```

**Step 2: 修改prepareUpload使用fetchWithAuth**

```javascript
async function prepareUpload() {
    const baseUrl = document.getElementById('baseUrl').value;
    const songName = document.getElementById('songName').value;
    const description = document.getElementById('description').value;
    const fileType = document.getElementById('fileType').value;

    updateStep('step2', 'in-progress', '2. 准备上传中...');

    const response = await fetchWithAuth(`${baseUrl}/api/songs/prepare`, {
        method: 'POST',
        body: JSON.stringify({
            mimeType: fileType,
            name: songName || '',
            description: description || null
        })
    });

    console.log('Prepare Response Status:', response.status);
    const data = await response.json();
    console.log('Prepare Response Data:', data);

    if (!data.success) {
        throw new Error(data.message || '准备上传失败');
    }

    updateStep('step2', 'success', '2. 准备上传成功');
    return data.data;
}
```

**Step 3: 修改completeUpload使用fetchWithAuth**

```javascript
async function completeUpload(songId) {
    const baseUrl = document.getElementById('baseUrl').value;

    updateStep('step4', 'in-progress', '4. 完成验证中...');

    const response = await fetchWithAuth(`${baseUrl}/api/songs/complete`, {
        method: 'POST',
        body: JSON.stringify({
            songId: songId
        })
    });

    const data = await response.json();

    if (!data.success) {
        throw new Error(data.message || '完成上传失败');
    }

    updateStep('step4', 'success', '4. 完成验证成功');
    return data.data;
}
```

**Step 4: 提交**

```bash
git add upload-test.html
git commit -m "添加401错误处理，使用fetchWithAuth包装fetch请求"
```

---

## Task 10: 添加登出功能

**Files:**
- Modify: `upload-test.html:107-119`（在登录区域添加登出按钮）

**Step 1: 在登录区域添加登出按钮和函数**

修改登录区域HTML：
```html
<div class="section">
    <h2>登录</h2>
    <div class="input-group">
        <label>邮箱:</label>
        <input type="email" id="email" placeholder="user@example.com">
    </div>
    <div class="input-group">
        <label>密码:</label>
        <input type="password" id="password" placeholder="password">
    </div>
    <button onclick="login()">登录</button>
    <button onclick="logout()" style="margin-left: 10px; background: #6c757d;">登出</button>
    <div id="loginStatus" class="status">未登录</div>
</div>
```

添加logout函数：
```javascript
function logout() {
    localStorage.removeItem('tokenName');
    localStorage.removeItem('tokenValue');
    const status = document.getElementById('loginStatus');
    status.innerHTML = '已登出';
    status.className = 'status';
}
```

**Step 2: 提交**

```bash
git add upload-test.html
git commit -m "添加登出功能，清除localStorage中的token"
```

---

## Task 11: 运行完整测试

**Files:**
- Test: All modified classes

**Step 1: 运行所有单元测试**

Run: `./mvnw test`
Expected: 所有测试通过

**Step 2: 测试前端登录流程**

1. 启动应用：`./mvnw spring-boot:run`
2. 打开 `upload-test.html`
3. 输入邮箱密码，点击登录
4. 验证 localStorage 中存在 tokenName 和 tokenValue
5. 验证页面显示"登录成功 - 用户名"

**Step 3: 测试前端上传流程**

1. 选择一个音频文件
2. 点击开始上传
3. 验证所有步骤成功完成
4. 验证显示正确的歌曲信息

**Step 4: 测试token过期处理**

1. 手动删除 localStorage 中的 token
2. 尝试上传文件
3. 验证弹出"登录已过期"提示
4. 验证 upload-test.html 显示错误信息

**Step 5: 提交**

```bash
git add .
git commit -m "完成header基于的鉴权改造，测试通过"
```

---

## 相关文档

- 设计文档: `docs/plans/2026-02-25-header-based-authentication-design.md`
- AGENTS.md: AudioFlow项目开发规范
- Sa-Token官方文档: 前后端分离（无Cookie模式）
