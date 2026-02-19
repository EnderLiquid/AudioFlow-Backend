## 使用框架

### 认证框架

- Sa-Token

### 持久层框架

- Mybatis-Plus

## 项目架构

### 项目共分为4层（自顶而下）

- Controller
- Service
- Manager
- Mapper

## 开发规范

### 架构规范

1.不得跨层访问。
2.底层不得访问顶层。
3.Manager层继承Mybatis-Plus的IService< Entity >接口和ServiceImpl< EntityMapper, Entity >类。
4.Service层不得使用QueryWrapper，所有查询条件在Manager层构建。
5.Service层不得创建Page，所有Page对象在Manager层构建。
6.在Service层完成所有参数校验。

### DTO规范

1.通过common/config/JacksonConfig和GlobalBindingAdvice类，分别实现非Get请求json反序列化时与Get请求绑定字段到DTO时，字符串自动trim，且若字符串为空则设为null。
2.DTO默认值的逻辑在service层实现，保持DTO纯洁。
3.DTO命名应以Entity名称作为前缀。

### Controller层规范

1.每个Controller类都要添加@Vaildated注解。
2.方法根据应用具体语义命名。
3.API 设计规范：
    - **HTTP 方法限制**：线上仅开放 `GET` 和 `POST`。
    - **Method Override**：所有写操作统一使用 `POST`。对于 `PUT`/`PATCH`/`DELETE` 语义，需在请求头中携带 `X-HTTP-Method-Override`。
    - **资源命名**：路径使用复数名词（如 `/api/songs`、`/api/users`），全小写，短横线分词。
    - **路径参数**：优先使用路径参数 `{id}` 定位资源。
    - **Session 管理**：登录/登出归入 `/api/sessions` 资源。


### Service层规范

1.每个Service接口类都要添加@Vaildated注解。
2.方法参数校验注释在接口类写。
3.方法参数中的DTO用@Vaild注解校验。
4.CRUD命名规范：

- 增：save
- 删：remove
- 改：update
- 查：get（单个）、list（多个）、page（分页）
  

### 日志规范

使用Lombok的@Slf4j注解注入日志对象。

#### 日志打印位置

- Service层方法入口：打印请求日志，包含关键参数。
- Service层方法出口：打印成功日志。
- 异常分支：Service层导致业务失败的异常抛出BusinessException，其他情况使用warn或error打印失败日志。

#### 日志内容格式

- 请求日志：`请求XXX，参数名: {}`。
- 成功日志：`XXX成功` 或 `XXX成功，关键信息: {}`。
