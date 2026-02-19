# Controller API 重写设计

## 背景与目标

当前 Controller 层接口风格混乱，且生产环境不允许 PUT/DELETE。目标是统一接口规范，在仅允许 GET/POST 的情况下保持清晰的资源语义，并确保运维侧可控。

## 约束与假设

- 仅开放 GET/POST。
- 允许自定义请求头，使用 `X-HTTP-Method-Override` 表达真实语义。
- 只重构 Controller 层，不改 Service/Manager 语义。
- 旧接口路径不兼容保留，直接切换到新规范。

## 方案对比

1. **REST-like + Method Override（推荐）**
   - 资源路径清晰，语义表达稳定。
   - 前端仅使用 GET/POST，对网关友好。
   - 通过 Header 兼容 PUT/PATCH/DELETE 语义。
2. RPC/Action 风格
   - 实现简单，但资源语义弱、接口扩展后易混乱。
3. Method Override 放在参数中
   - 兼容性高，但可读性弱，且易被缓存/代理误判。

## 设计细节

### 1) 路径与资源命名

- 统一前缀：`/api`
- 资源命名：复数名词、全小写，短横线分词（如 `/api/songs`）
- 禁止在路径里出现动词（除子资源/动作约定外）
- 具体约定：
  - 集合查询：`GET /api/songs`（query 参数承载筛选/分页）
  - 单个资源：`GET /api/songs/{id}`
  - 创建：`POST /api/songs`
  - 更新/删除：`POST /api/songs/{id}` + `X-HTTP-Method-Override`

### 2) Method Override 语义

- 仅 `POST` 接受 `X-HTTP-Method-Override`，其取值：`PUT`/`PATCH`/`DELETE`。
- `POST /api/{resources}`：创建（无 Override）。
- `POST /api/{resources}/{id}`：
  - `PUT` 全量替换
  - `PATCH` 部分更新
  - `DELETE` 删除

### 3) 请求/响应规范

- `GET` 只走 query，不使用 body。
- 写操作 `POST` 使用 JSON (`application/json`)；文件上传使用 `multipart/form-data`。
- query 参数统一 lowerCamel。
- 响应统一 `HttpResponseBody<T>`：
  - 成功：`success=true`，`message` 可空
  - 失败：`success=false`，`message` 必填
- 流式/重定向接口可为非标准响应（明确标注）。

### 4) 鉴权/会话接口

- 登录：`POST /api/sessions`
- 退出当前会话：`POST /api/sessions/current` + Override=DELETE
- 注册：`POST /api/users`
- 当前用户：`GET /api/users/me`
- 修改密码：
  - 自己：`POST /api/users/me/password` + Override=PATCH
  - 管理员：`POST /api/users/{id}/password` + Override=PATCH

## 影响范围

- Controller 路由与方法签名调整。
- 新增 Method Override Filter。
- 增加 SessionController，拆分用户会话接口。

## 测试策略

- 运行现有 Spring Boot 测试，确保上下文可启动。
- 覆盖关键接口的手工调用（登录/退出/歌曲 CRUD/播放）。
