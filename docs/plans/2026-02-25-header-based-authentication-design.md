# Header基于的鉴权改造设计文档

## 概述

将AudioFlow项目从Cookie模式的Sa-Token鉴权改造为前端手动在请求头中携带Token的无Cookie模式，以避免SameSite等复杂的Cookie相关问题。

## 背景与目标

**背景**：
- 当前项目使用Sa-Token的Cookie模式进行鉴权
- 测试页面upload-test.html使用`credentials: 'include'`携带Cookie
- Cookie的SameSite属性导致跨域场景下的鉴权问题

**目标**：
- 完全禁用Cookie模式，改为前端手动在header中携带token
- 登录和注册接口返回完整的token信息
- 前端将token存储在localStorage，后续请求在header中携带
- 改造测试页面upload-test.html以适配新的鉴权方式

## 设计方案

### 1. 数据结构设计

**新增LoginResult类**

位置：`top.enderliquid.audioflow.dto.response.LoginResult`

```java
import cn.dev33.satoken.stp.SaTokenInfo;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LoginResult {
    private UserVO user;
    private SaTokenInfo tokenInfo;
}
```

**依赖说明**：
- `UserVO`：已存在的用户信息响应对象
- `SaTokenInfo`：Sa-Token提供的token信息封装类，包含：
  - `tokenName`：token名称（默认为"satoken"）
  - `tokenValue`：token值
  - `timeout`：token过期时间（秒）
  - 等其他token相关属性

### 2. 后端接口修改

#### 2.1 SessionController.login

**位置**：`src/main/java/top/enderliquid/audioflow/controller/SessionController.java`

**修改前**：
```java
public HttpResponseBody<UserVO> login(@Valid @RequestBody UserVerifyPasswordDTO dto) {
    UserVO userVO = userService.verifyUserPassword(dto);
    StpUtil.login(userVO.getId());
    return HttpResponseBody.ok(userVO, "登录成功");
}
```

**修改后**：
```java
public HttpResponseBody<LoginResult> login(@Valid @RequestBody UserVerifyPasswordDTO dto) {
    UserVO userVO = userService.verifyUserPassword(dto);
    StpUtil.login(userVO.getId());
    SaTokenInfo tokenInfo = StpUtil.getTokenInfo();
    return HttpResponseBody.ok(new LoginResult(userVO, tokenInfo), "登录成功");
}
```

#### 2.2 UserController.register

**位置**：`src/main/java/top/enderliquid/audioflow/controller/UserController.java`

**修改前**：
```java
public HttpResponseBody<UserVO> register(@Valid @RequestBody UserSaveDTO dto) {
    UserVO userVO = userService.saveUser(dto);
    StpUtil.login(userVO.getId());
    return HttpResponseBody.ok(userVO, "注册成功");
}
```

**修改后**：
```java
public HttpResponseBody<LoginResult> register(@Valid @RequestBody UserSaveDTO dto) {
    UserVO userVO = userService.saveUser(dto);
    StpUtil.login(userVO.getId());
    SaTokenInfo tokenInfo = StpUtil.getTokenInfo();
    return HttpResponseBody.ok(new LoginResult(userVO, tokenInfo), "注册成功");
}
```

#### 2.3 SessionController.logout

无需修改，仍使用`StpUtil.logoutByTokenValue(StpUtil.getTokenValue())`
Sa-Token会自动从header中读取token进行注销。

### 3. 前端修改（upload-test.html）

#### 3.1 登录函数

**位置**：`upload-test.html` 第206-234行

**修改前**：
```javascript
fetch(`${baseUrl}/api/sessions`, {
    method: 'POST',
    headers: {
        'Content-Type': 'application/json'
    },
    body: JSON.stringify({
        email: email,
        password: password
    }),
    credentials: 'include'
}).then(response => {
    // ...
    return response.json();
}).then(data => {
    if (data.success) {
        status.innerHTML = `登录成功 - ${data.data.name}`;
    }
    // ...
});
```

**修改后**：
```javascript
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
        // 存储token到localStorage
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
```

#### 3.2 token携带函数

**新增辅助函数**：
```javascript
// 获取携带token的headers
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

#### 3.3 准备上传、完成上传等函数修改

所有需要鉴权的API调用都需要：
1. 使用`getAuthHeaders()`获取携带token的headers
2. 移除`credentials: 'include'`选项

**示例（prepareUpload）**：
```javascript
const response = await fetch(`${baseUrl}/api/songs/prepare`, {
    method: 'POST',
    headers: getAuthHeaders(),
    body: JSON.stringify({
        mimeType: fileType,
        name: songName || '',
        description: description || null
    })
});
```

### 4. 配置修改

**禁用Cookie读取**（可选，但推荐）：

在`application.properties`中添加：
```properties
sa-token.is-read-cookie=false
```

这样可以确保Sa-Token完全依赖header中的token进行鉴权，不会从Cookie中读取。

### 5. 数据流

#### 5.1 登录流程

```
用户                    前端                后端                 Sa-Token
 |                        |                   |                      |
 |-- 输入邮箱密码 -------->|                   |                      |
 |                        |                   |                      |
 |                        |-- POST /api/sessions (email, password) ->|
 |                        |                   |                      |
 |                        |<-- 验证成功 ------|                      |
 |                        |                   |-- StpUtil.login(userId) ->|
 |                        |                   |                      |
 |                        |                   |<-- SaTokenInfo ------|
 |                        |                   |                      |
 |                        |<-- {success: true, data: {user, tokenInfo}} |
 |                        |                   |                      |
 |                        |-- 存储token到localStorage -------|
 |                        |                   |                      |
 |<-- 显示登录成功 --------|                   |                      |
```

#### 5.2 鉴权流程

```
用户                    前端                后端                 Sa-Token
 |                        |                   |                      |
 |-- 访问受保护接口------->|                   |                      |
 |                        |                   |                      |
 |                        |-- 读取localStorage -->|
 |                        |                   |                      |
 |                        |-- POST /api/songs/prepare |
 |                        |    headers: { satoken: "...", Content-Type: "..." } |
 |                        |------------------>|                      |
 |                        |                   |-- 从header读取token ->|
 |                        |                   |                      |
 |                        |                   |<-- 验证通过 ---------|
 |                        |                   |                      |
 |                        |<-- 业务响应 ------|                      |
 |                        |                   |                      |
 |<-- 显示结果 -----------|                   |                      |
```

### 6. 错误处理

#### 6.1 后端错误处理

**登录/注册失败**：
- Service层抛出`BusinessException`
- `GlobalExceptionHandler`统一处理
- 响应格式：`{"success": false, "message": "错误信息", "data": null}`
- 不创建token，不返回LoginResult

**token无效/过期**：
- Sa-Token自动检测，返回401未授权
- `GlobalExceptionHandler`处理401异常
- 响应格式：`{"success": false, "message": "未登录", "data": null}`

#### 6.2 前端错误处理

**登录/注册失败**：
```javascript
.then(data => {
    if (data.success) {
        // 成功处理
    } else {
        // 提示错误信息
        status.innerHTML = `登录失败: ${data.message}`;
    }
})
```

**token过期或无效**：
```javascript
fetch(url, {
    headers: getAuthHeaders()
}).then(response => {
    if (response.status === 401) {
        // 清除localStorage中的token
        localStorage.removeItem('tokenName');
        localStorage.removeItem('tokenValue');
        // 提示需要重新登录
        alert('登录已过期，请重新登录');
    }
    return response.json();
})
```

### 7. 测试策略

#### 7.1 后端单元测试

**SessionControllerTest**：
- 测试登录接口返回LoginResult而非UserVO
- 验证tokenInfo字段包含正确的tokenName和tokenValue
- 验证登录失败时返回正确的错误信息

**UserControllerTest**：
- 测试注册接口返回LoginResult而非UserVO
- 验证注册后自动登录，tokenInfo字段正确
- 验证注册失败时返回正确的错误信息

#### 7.2 前端集成测试

**登录测试**：
- 测试登录成功后token正确存储到localStorage
- 验证localStorage中存在正确的tokenName和tokenValue

**鉴权测试**：
- 测试携带token可以正常访问受保护接口
- 测试未携带token访问受保护接口返回401
- 测试token过期后访问受保护接口返回401

**登出测试**：
- 测试登出成功后清除localStorage中的token

#### 7.3 完整流程测试

**注册流程**：
1. 注册新用户 → 验证返回LoginResult
2. 提取token并存储
3. 使用token调用需要鉴权的接口（如上传）
4. 验证接口正常工作

**登录流程**：
1. 使用已存在的账号登录
2. 验证返回LoginResult
3. 使用token调用需要鉴权的接口
4. 验证接口正常工作

**失败场景**：
1. 不携带token访问受保护接口 → 验证返回401
2. 使用错误token访问受保护接口 → 验证返回401
3. token过期后访问受保护接口 → 验证返回401

## 实施计划

详细的实施步骤将在下一阶段使用`writing-plans`技能生成实现计划。

## 风险与注意事项

1. **向后兼容性**：登录和注册接口的返回类型从`UserVO`改为`LoginResult`，客户端需要同步更新
2. **测试数据影响**：现有的测试用例需要更新以适配新的返回格式
3. **Sa-Token配置**：添加`sa-token.is-read-cookie=false`后，可能影响其他依赖Cookie的功能
4. **localStorage安全**：token存储在localStorage中，需要注意XSS攻击风险（测试页面可以接受）

## 参考文档

- Sa-Token官方文档：前后端分离（无Cookie模式）
- AGENTS.md：AudioFlow项目开发规范
