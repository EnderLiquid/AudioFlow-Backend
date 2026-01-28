## 使用框架

**认证框架**

Sa-Token

**持久层框架**

Mybatis-Plus

## 项目架构

**项目共分为4层（自顶而下）**

Controller

Service

Manager

Mapper

## 开发规范

**架构规范**

1.不得跨层访问。

2.底层不得访问顶层。

3.Manager层继承Mybatis-Plus的IService< Entity >接口和ServiceImpl< EntityMapper, Entity >类。

4.Service层不得使用QueryWrapper，所有查询条件在Manager层构建。

5.Service层不得创建Page，所有Page对象在Manager层构建。

6.在Service层完成所有参数校验。

**DTO规范**

1.通过common/config/JacksonConfig和GlobalBindingAdvice类，分别实现非Get请求json反序列化时与Get请求绑定字段到DTO时，字符串自动trim，且若字符串为空则设为null。

2.DTO默认值的逻辑在service层实现，保持DTO纯洁。

3.DTO命名应以Entity名称作为前缀。

**Controller层规范**

1.每个Controller类都要添加@Vaildated注解。

2.方法参数中的DTO用@Vaild注解校验。

3.方法根据应用具体语义命名。

**Service层规范**

1.每个Service接口类都要添加@Vaildated注解。

2.方法参数校验注释在接口类写。

3.方法参数中的DTO用@Vaild注解校验。

4.CRUD命名规范：

增：save

删：remove

改：update

查：get（单个）、list（多个）、page（分页）
