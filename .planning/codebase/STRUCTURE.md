# Codebase Structure

**Analysis Date:** 2026-03-02

## Directory Layout

```
[project-root]/
├── config/                     # 外部配置文件（敏感配置，gitignored）
│   ├── application-dev.properties
│   └── application-prod.properties
├── docs/                       # 项目文档
│   └── plans/                  # 功能设计文档和实现计划
├── logs/                       # 运行时日志输出目录
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── top/enderliquid/audioflow/
│   │   │       ├── AudioFlowApplication.java    # Spring Boot 入口类
│   │   │       ├── common/                      # 公共组件和配置
│   │   │       │   ├── annotation/              # 自定义注解
│   │   │       │   ├── config/                  # 配置类（Spring配置、Jackson等）
│   │   │       │   ├── enums/                   # 枚举类型
│   │   │       │   ├── exception/               # 异常类和全局异常处理
│   │   │       │   ├── filter/                  # Servlet过滤器
│   │   │       │   ├── handler/                 # MyBatis-Plus处理器
│   │   │       │   ├── response/                # 统一响应封装
│   │   │       │   ├── task/                    # 定时任务和启动任务
│   │   │       │   └── util/                    # 工具类
│   │   │       │       └── id/                  # ID转换工具
│   │   │       ├── controller/                  # REST API控制器层
│   │   │       ├── dto/                         # 数据传输对象
│   │   │       │   ├── bo/                      # Business Object（业务对象）
│   │   │       │   ├── param/                   # Manager层参数对象
│   │   │       │   ├── request/                 # 请求DTO
│   │   │       │   │   ├── song/                # 歌曲相关请求DTO
│   │   │       │   │   └── user/                # 用户相关请求DTO
│   │   │       │   └── response/                # 响应VO
│   │   │       │       ├── session/             # 会话相关响应VO
│   │   │       │       ├── song/                # 歌曲相关响应VO
│   │   │       │       └── user/                # 用户相关响应VO
│   │   │       ├── entity/                      # 数据库实体类
│   │   │       ├── manager/                     # 数据访问层（MyBatis-Plus Service）
│   │   │       │   └── impl/                    # Manager实现类
│   │   │       ├── mapper/                      # MyBatis Mapper接口
│   │   │       └── service/                     # 业务逻辑层
│   │   │           └── impl/                    # Service实现类
│   │   └── resources/
│   │       ├── application.properties           # 基础配置文件
│   │       ├── application-dev.properties       # 开发环境配置
│   │       ├── application-prod.properties      # 生产环境配置
│   │       ├── mapper/                          # MyBatis XML映射文件
│   │       ├── scripts/                         # SQL脚本
│   │       ├── sql/                             # SQL文件
│   │       └── static/                          # 静态资源
│   └── test/
│       ├── java/top/enderliquid/audioflow/
│       │   ├── benchmark/                       # 基准测试
│       │   ├── common/                          # 测试公共类
│       │   ├── config/                          # 测试配置
│       │   └── controller/                      # 控制器测试
│       └── resources/
│           └── application.properties           # 测试配置
├── pom.xml                     # Maven构建配置
└── mvnw/mvnw.cmd               # Maven Wrapper
```

## Directory Purposes

**`src/main/java/top/enderliquid/audioflow/common/`:**
- Purpose: 存放所有公共组件、配置和基础设施代码
- Contains: 配置类、过滤器、异常处理、工具类、注解、枚举
- Key files:
  - `config/WebMvcConfig.java`: Web MVC配置、CORS、拦截器
  - `config/MybatisPlusConfig.java`: MyBatis-Plus分页配置
  - `config/JacksonConfig.java`: JSON反序列化配置（字符串trim）
  - `config/SecurityConfig.java`: 密码加密配置
  - `config/GlobalBindingAdvice.java`: 表单数据绑定处理
  - `config/RateLimitAspect.java`: 限流切面
  - `exception/GlobalExceptionHandler.java`: 全局异常处理器
  - `exception/ExceptionTranslator.java`: 异常翻译器
  - `filter/HttpMethodOverrideFilter.java`: HTTP方法重写过滤器
  - `filter/RequestIdFilter.java`: 请求ID过滤器
  - `handler/MyMetaObjectHandler.java`: 自动填充处理器
  - `task/SongUploadCleanupTask.java`: 上传清理定时任务
  - `task/LogCleanupRunner.java`: 日志清理启动任务

**`src/main/java/top/enderliquid/audioflow/controller/`:**
- Purpose: REST API端点定义
- Contains: Controller类，处理HTTP请求/响应
- Key files:
  - `UserController.java`: 用户注册、信息查询、密码修改
  - `SessionController.java`: 登录/注销会话管理
  - `SongController.java`: 歌曲上传、查询、删除、播放

**`src/main/java/top/enderliquid/audioflow/service/`:**
- Purpose: 业务逻辑层接口定义
- Contains: Service接口，声明业务操作契约
- Key files:
  - `UserService.java`: 用户业务接口
  - `SongService.java`: 歌曲业务接口
  - `RateLimitService.java`: 限流服务接口

**`src/main/java/top/enderliquid/audioflow/service/impl/`:**
- Purpose: 业务逻辑层实现
- Contains: Service实现类，包含业务规则、事务管理、参数验证
- Key files:
  - `UserServiceImpl.java`: 用户业务实现
  - `SongServiceImpl.java`: 歌曲业务实现（含音频元数据解析）
  - `RateLimitServiceImpl.java`: 令牌桶限流实现
  - `StpInterfaceImpl.java`: Sa-Token权限接口实现

**`src/main/java/top/enderliquid/audioflow/manager/`:**
- Purpose: 数据访问层（DAO层）
- Contains: Manager接口，继承MyBatis-Plus的IService
- Key files:
  - `UserManager.java`: 用户数据访问接口
  - `SongManager.java`: 歌曲数据访问接口
  - `OSSManager.java`: 对象存储抽象接口
  - `RedisManager.java`: Redis操作接口

**`src/main/java/top/enderliquid/audioflow/manager/impl/`:**
- Purpose: 数据访问层实现
- Contains: Manager实现类，包含QueryWrapper构造和分页逻辑
- Key files:
  - `UserManagerImpl.java`: 用户数据访问实现
  - `SongManagerImpl.java`: 歌曲数据访问实现（含自定义分页查询）
  - `OSSManagerImpl.java`: S3 SDK对象存储实现
  - `RedisManagerImpl.java`: Redis操作实现

**`src/main/java/top/enderliquid/audioflow/mapper/`:**
- Purpose: MyBatis Mapper接口定义
- Contains: 继承BaseMapper的接口
- Key files:
  - `UserMapper.java`: 用户表Mapper
  - `SongMapper.java`: 歌曲表Mapper

**`src/main/java/top/enderliquid/audioflow/entity/`:**
- Purpose: 数据库实体类
- Contains: 与数据库表对应的POJO类
- Key files:
  - `User.java`: 用户实体
  - `Song.java`: 歌曲实体

**`src/main/java/top/enderliquid/audioflow/dto/`:**
- Purpose: 数据传输对象，分层传递数据
- Subdirectories:
  - `bo/`: Business Object，用于Manager层返回复杂查询结果
  - `param/`: Manager层参数对象，封装查询条件
  - `request/`: 请求DTO，接收前端传入数据
  - `response/`: 响应VO，返回给前端的数据

**`src/main/resources/mapper/`:**
- Purpose: MyBatis XML映射文件
- Contains: 自定义SQL查询定义
- Key files:
  - `SongMapper.xml`: 歌曲分页查询SQL（JOIN用户表）

**`config/`:**
- Purpose: 外部配置文件（敏感配置，不被git跟踪）
- Contains: 各环境的properties配置文件
- Usage: 优先级高于classpath内的配置文件

## Key File Locations

**Entry Points:**
- `src/main/java/top/enderliquid/audioflow/AudioFlowApplication.java`: Spring Boot应用入口

**Configuration:**
- `src/main/resources/application.properties`: 基础配置
- `src/main/resources/application-dev.properties`: 开发环境配置
- `src/main/resources/application-prod.properties`: 生产环境配置
- `config/application-dev.properties`: 外部开发配置（优先）
- `config/application-prod.properties`: 外部生产配置（优先）

**Core Logic:**
- `service/impl/UserServiceImpl.java`: 用户核心业务逻辑
- `service/impl/SongServiceImpl.java`: 歌曲核心业务逻辑（含音频解析）
- `manager/impl/SongManagerImpl.java`: 歌曲数据访问（分页查询）
- `common/config/RateLimitAspect.java`: 限流切面逻辑
- `common/exception/ExceptionTranslator.java`: 异常分类处理

**Testing:**
- `src/test/java/top/enderliquid/audioflow/config/BaseControllerTest.java`: 测试基类
- `src/test/java/top/enderliquid/audioflow/common/TestDataHelper.java`: 测试数据辅助
- `src/test/java/top/enderliquid/audioflow/controller/`: 控制器测试
- `src/test/resources/application.properties`: 测试专用配置

## Naming Conventions

**Files:**
- Controller: `[Entity]Controller.java` (e.g., `UserController.java`)
- Service Interface: `[Entity]Service.java` (e.g., `UserService.java`)
- Service Implementation: `[Entity]ServiceImpl.java` (e.g., `UserServiceImpl.java`)
- Manager Interface: `[Entity]Manager.java` (e.g., `UserManager.java`)
- Manager Implementation: `[Entity]ManagerImpl.java` (e.g., `UserManagerImpl.java`)
- Mapper: `[Entity]Mapper.java` (e.g., `UserMapper.java`)
- Entity: `[Entity].java` (e.g., `User.java`, `Song.java`)
- DTO Request: `[Action][Entity]DTO.java` or `[Entity][Action]DTO.java` (e.g., `UserSaveDTO.java`, `SongPageDTO.java`)
- VO Response: `[Entity]VO.java` (e.g., `UserVO.java`, `SongVO.java`)
- BO: `[Entity]BO.java` (e.g., `SongBO.java`)
- Param: `[Entity][Action]Param.java` (e.g., `SongPageParam.java`)

**Directories:**
- 全部小写，使用连字符分隔（但在Java包中使用驼峰）
- 按功能分组：`controller/`, `service/`, `manager/`, `mapper/`
- DTO子目录按用途分：`request/`, `response/`, `bo/`, `param/`

## Where to Add New Code

**New Entity (Database Table):**
1. Entity: `src/main/java/top/enderliquid/audioflow/entity/[Entity].java`
2. Mapper Interface: `src/main/java/top/enderliquid/audioflow/mapper/[Entity]Mapper.java`
3. Manager Interface: `src/main/java/top/enderliquid/audioflow/manager/[Entity]Manager.java`
4. Manager Implementation: `src/main/java/top/enderliquid/audioflow/manager/impl/[Entity]ManagerImpl.java`
5. Service Interface: `src/main/java/top/enderliquid/audioflow/service/[Entity]Service.java`
6. Service Implementation: `src/main/java/top/enderliquid/audioflow/service/impl/[Entity]ServiceImpl.java`
7. Controller: `src/main/java/top/enderliquid/audioflow/controller/[Entity]Controller.java`
8. Tests: `src/test/java/top/enderliquid/audioflow/controller/[Entity]ControllerTest.java`

**New API Endpoint:**
- Controller method in appropriate `*Controller.java`
- DTO: `src/main/java/top/enderliquid/audioflow/dto/request/[entity]/[Action]DTO.java`
- VO: `src/main/java/top/enderliquid/audioflow/dto/response/[entity]/[Result]VO.java`
- Service method in interface and implementation
- Manager method if custom query needed

**New Custom Query:**
- Add method to `*[Entity]Manager.java` interface
- Implement in `*[Entity]ManagerImpl.java` using QueryWrapper
- For complex SQL: add to `src/main/resources/mapper/[Entity]Mapper.xml`

**New Configuration:**
- Spring Config: `src/main/java/top/enderliquid/audioflow/common/config/[Name]Config.java`
- Properties: add to `application.properties` or environment-specific file

**New Utility:**
- Static utility: `src/main/java/top/enderliquid/audioflow/common/util/[Name].java`
- Component: `src/main/java/top/enderliquid/audioflow/common/util/[Name].java` with `@Component`

**New Filter/Interceptor:**
- Filter: `src/main/java/top/enderliquid/audioflow/common/filter/[Name]Filter.java`
- Register in `WebMvcConfig.java`

**New Scheduled Task:**
- Add to `src/main/java/top/enderliquid/audioflow/common/task/[Name]Task.java` with `@Scheduled`

**Tests:**
- Controller tests: `src/test/java/top/enderliquid/audioflow/controller/[Entity]ControllerTest.java`
- Extend `BaseControllerTest` for common setup
- Use `TestDataHelper` for test data creation

## Special Directories

**`config/`:**
- Purpose: 外部配置文件存储
- Generated: No
- Committed: No (in .gitignore)
- Priority: 最高（覆盖classpath内配置）

**`logs/`:**
- Purpose: 应用程序日志输出
- Generated: Yes (运行时创建)
- Committed: No (in .gitignore)

**`target/`:**
- Purpose: Maven构建输出
- Generated: Yes
- Committed: No (in .gitignore)

**`docs/plans/`:**
- Purpose: 功能设计文档和实现计划
- Generated: No
- Committed: Yes

---

*Structure analysis: 2026-03-02*
