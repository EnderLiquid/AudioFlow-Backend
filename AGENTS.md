# AGENTS.md - AudioFlow 代理指南

## 项目概述

AudioFlow 是一个基于 Java 21 和 Spring Boot 的应用程序，使用 Maven 构建系统。

## 构建命令

```bash
# 编译项目
./mvnw clean compile

# 构建项目
./mvnw clean package

# 运行应用程序
./mvnw spring-boot:run

# 运行所有测试
./mvnw test

# 运行单个测试类
./mvnw test -Dtest=HttpMethodOverrideFilterTest

# 运行单个测试方法
./mvnw test -Dtest=HttpMethodOverrideFilterTest#shouldOverrideMethodWhenHeaderPresentInPost

# 构建时跳过测试
./mvnw clean package -DskipTests
```

## 项目架构

四层架构（自上而下）：

- **Controller**: REST API 端点
- **Service**: 业务逻辑层
- **Manager**: 数据访问层（继承 MyBatis-Plus IService）
- **Mapper**: 数据库映射层

### 架构规则

1. 禁止跨层访问（Controller 不能直接访问 Manager）
2. 禁止自底向上访问
3. Manager 层继承 MyBatis-Plus 接口
4. Service 层不得使用 QueryWrapper（条件在 Manager 层构建）
5. Service 层不得创建 Page 对象（在 Manager 层构建）
6. 所有参数校验在 Service 层完成

## 代码风格规范

### 命名规范

- **类名**: PascalCase（例如：`UserController`, `UserService`）
- **方法名**: camelCase，具有语义含义
- **DTO 命名**: 实体名前缀（例如：`UserSaveDTO`, `SongUpdateDTO`）
- **CRUD 命名**: `save`（创建）、`remove`（删除）、`update`（修改）、`get`（单个查询）、`list`（多个查询）、`page`（分页查询）

### 注解规范

- 所有 Controller: 类级别添加 `@Validated`
- 所有 Service 接口: 类级别添加 `@Validated`
- DTO 参数: 使用 `@Valid` 进行校验
- 日志: 使用 `@Slf4j`（Lombok）

### API 设计规范

- **HTTP 方法**: 生产环境仅使用 GET 和 POST
- **方法重写**: 使用 `X-HTTP-Method-Override` 请求头在 POST 上实现 PUT/PATCH/DELETE 语义
- **资源路径**: 使用复数名词、小写、短横线分隔（例如：`/api/users`, `/api/songs`）
- **路径参数**: 使用 `{id}` 标识资源
- **会话管理**: 登录/注销使用 `/api/sessions`

### DTO 规范

1. 通过 `JacksonConfig` 和 `GlobalBindingAdvice` 实现字符串自动 trim
2. 默认值逻辑在 Service 层实现，保持 DTO 纯洁
3. 命名规则：实体前缀（例如：`UserSaveDTO`, `SongPageDTO`）

### 错误处理规范

- 业务失败抛出 `BusinessException`
- 成功响应使用 `HttpResponseBody.ok()`
- 失败响应使用 `HttpResponseBody.fail()`
- 全局异常处理器位于 `GlobalExceptionHandler`

### 语言要求

- **所有代码注释必须使用中文**
- **所有日志消息必须使用中文**
- **所有异常消息必须使用中文**
- **所有 Git 提交消息必须使用中文**

### 类型声明规范

- **禁止使用 `var` 关键字**：所有变量必须使用显式类型声明，禁止使用 Java 10+ 的 `var` 类型推断特性。这包括：
  - 局部变量声明
  - 增强 for 循环变量
  - try-with-resources 变量
  - lambda 表达式参数（除非显式指定类型）
- **显式类型声明**提高了代码可读性和可维护性，特别是在代码审查和调试时

### 日志格式规范

- 入口日志：`请求XXX，参数名: {}`
- 成功日志：`XXX成功` 或 `XXX成功，关键信息: {}`
- 意外失败使用 `warn`/`error`
- 预期业务失败使用 `BusinessException`

# 

## 技术栈

- **Java**: 21
- **Spring Boot**: 3.5.10
- **认证**: Sa-Token
- **持久层**: MyBatis-Plus
- **校验**: Jakarta Validation
- **工具库**: Lombok, Apache Tika, AWS S3 SDK

## 测试规范

- 测试框架: JUnit 5
- 测试位置: `src/test/java`
- 使用 Spring 的 `MockHttpServletRequest` 进行 Servlet 测试
- 使用 Mockito 进行依赖模拟
- 测试方法命名应具有描述性（例如：`shouldOverrideMethodWhenHeaderPresentInPost`）

## 其他资源

详见 `DEVELOPMENT.md` 了解详细的开发规范。
