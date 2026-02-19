# Controller API 重写 Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 将 Controller 层接口统一为 REST-like + `X-HTTP-Method-Override` 规范，并移除旧路径。

**Architecture:** 在 Web 层新增 Method Override Filter，将 POST 请求中的 `X-HTTP-Method-Override` 转换为真实 HTTP 方法；重构 Controller 路由为 `/api/{resources}` 复数资源路径，并拆分用户会话接口。

**Tech Stack:** Spring Boot 3.5.x, Sa-Token, MyBatis-Plus

---

### Task 1: 新增 Method Override Filter

**Files:**
- Create: `src/main/java/top/enderliquid/audioflow/common/config/HttpMethodOverrideFilter.java`
- Modify: `src/main/java/top/enderliquid/audioflow/common/config/WebMvcConfig.java`

**Step 1: Write the failing test**

```java
// Controller 层暂未改动，先跳过测试（当前项目无控制层测试基架）
```

**Step 2: Run test to verify it fails**

Run: `./mvnw -q test`
Expected: PASS (无测试覆盖，作为基线确认)

**Step 3: Write minimal implementation**

```java
@Component
public class HttpMethodOverrideFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(...) {
        // 仅对 POST 生效
        // 读取 X-HTTP-Method-Override，允许 PUT/PATCH/DELETE
        // 包装 HttpServletRequest，覆盖 getMethod()
    }
}
```

在 `WebMvcConfig` 中注册过滤器顺序（优先于 Spring MVC 处理）。

**Step 4: Run test to verify it passes**

Run: `./mvnw -q test`
Expected: PASS

**Step 5: Commit**

```bash
        src/main/java/top/enderliquid/audioflow/common/config/WebMvcConfig.java
git commit -m "feat: add http method override filter"
```

---

### Task 2: 重构 UserController 为 Users + Sessions

**Files:**
- Modify: `src/main/java/top/enderliquid/audioflow/controller/UserController.java`
- Create: `src/main/java/top/enderliquid/audioflow/controller/SessionController.java`

**Step 1: Write the failing test**

```java
// 暂不新增测试
```

**Step 2: Run test to verify it fails**

Run: `./mvnw -q test`
Expected: PASS

**Step 3: Write minimal implementation**

- `UserController` 改为 `/api/users`：
  - `POST /api/users` 注册
  - `GET /api/users/me` 获取当前用户
  - `PATCH /api/users/me/password` 修改密码
- `SessionController` 新增 `/api/sessions`：
  - `POST /api/sessions` 登录
  - `DELETE /api/sessions/current` 退出当前会话

**Step 4: Run test to verify it passes**

Run: `./mvnw -q test`
Expected: PASS

**Step 5: Commit**

```bash
        src/main/java/top/enderliquid/audioflow/controller/SessionController.java
git commit -m "refactor: split users and sessions controllers"
```

---

### Task 3: 重构 SongController 为复数资源路径

**Files:**
- Modify: `src/main/java/top/enderliquid/audioflow/controller/SongController.java`

**Step 1: Write the failing test**

```java
// 暂不新增测试
```

**Step 2: Run test to verify it fails**

Run: `./mvnw -q test`
Expected: PASS

**Step 3: Write minimal implementation**

- `POST /api/songs` 上传
- `GET /api/songs` 分页/搜索
- `GET /api/songs/{id}` 获取详情
- `GET /api/songs/{id}/play` 获取播放 URL（重定向）
- `DELETE /api/songs/{id}` 删除（普通用户）
- `DELETE /api/songs/{id}/force` 管理员强制删除
- `PATCH /api/songs/{id}` 更新（普通用户）
- `PATCH /api/songs/{id}/force` 管理员更新

**Step 4: Run test to verify it passes**

Run: `./mvnw -q test`
Expected: PASS

**Step 5: Commit**

```bash
git commit -m "refactor: align song endpoints with new api spec"
```

---

### Task 4: 更新 DEVELOPMENT.md API 规范

**Files:**
- Modify: `DEVELOPMENT.md`

**Step 1: Write the failing test**

```text
// 文档更新，无测试
```

**Step 2: Run test to verify it fails**

Run: `./mvnw -q test`
Expected: PASS

**Step 3: Write minimal implementation**

在 “Controller 层规范” 增加：
- 仅允许 GET/POST
- Method Override 规则
- 资源命名与路径约束

**Step 4: Run test to verify it passes**

Run: `./mvnw -q test`
Expected: PASS

**Step 5: Commit**

```bash
git commit -m "docs: add controller api specification"
```
