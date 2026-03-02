# Architecture

**Analysis Date:** 2026-03-02

## Pattern Overview

**Overall:** Layered Architecture with Four-Layer Design

This is a Spring Boot-based REST API application following a strict four-layer architecture pattern. The codebase implements a music/audio file management system with user authentication, song upload/download, and pagination capabilities.

**Key Characteristics:**
- Strict layer separation with no cross-layer access
- MyBatis-Plus for ORM with custom SQL support
- Sa-Token for stateless authentication
- S3-compatible object storage for file management
- Token bucket rate limiting via AOP
- Global exception handling with standardized responses

## Layers

**Controller Layer:**
- Purpose: Handle HTTP requests/responses, input validation triggering, authentication/authorization checks
- Location: `src/main/java/top/enderliquid/audioflow/controller/`
- Contains: REST endpoints, `@RestController` classes, request mapping definitions
- Depends on: Service layer only
- Used by: External HTTP clients
- Key files:
  - `UserController.java`: User registration, profile retrieval, password change
  - `SessionController.java`: Login/logout session management
  - `SongController.java`: Song upload (prepare/complete), search, deletion, playback URL generation

**Service Layer:**
- Purpose: Business logic implementation, parameter validation, transaction management
- Location: `src/main/java/top/enderliquid/audioflow/service/` (interfaces) and `service/impl/` (implementations)
- Contains: Business service interfaces and implementations with `@Service` annotation
- Depends on: Manager layer only
- Used by: Controller layer
- Key patterns:
  - All methods log entry with parameters: `log.info("请求XXX，参数名: {}", param);`
  - Success exit logging: `log.info("XXX成功");`
  - Business failures throw `BusinessException`
  - Parameter validation via `@Valid` and `@NotNull`

**Manager Layer:**
- Purpose: Data access abstraction, QueryWrapper construction, Page object creation
- Location: `src/main/java/top/enderliquid/audioflow/manager/` (interfaces) and `manager/impl/` (implementations)
- Contains: Data access interfaces extending `IService<Entity>` and implementations extending `ServiceImpl<Mapper, Entity>`
- Depends on: Mapper layer
- Used by: Service layer
- Key responsibilities:
  - All database query construction happens here
  - Pagination logic implemented here
  - Example: `SongManagerImpl.pageByUploaderKeywordAndSongKeyword()` builds complex queries with LambdaQueryWrapper

**Mapper Layer:**
- Purpose: Database mapping layer, MyBatis XML query definitions
- Location: `src/main/java/top/enderliquid/audioflow/mapper/` (interfaces) and `resources/mapper/` (XML files)
- Contains: MyBatis mapper interfaces annotated with `@Mapper` and corresponding XML files
- Depends on: Database
- Used by: Manager layer
- Key files:
  - `UserMapper.java`: Basic CRUD operations via MyBatis-Plus BaseMapper
  - `SongMapper.java` + `SongMapper.xml`: Custom SQL for paginated song search with user join

## Data Flow

**Standard Request Flow:**

1. **HTTP Request** arrives at Controller method with `@RateLimit` and `@SaCheckLogin` annotations
2. **Filter Chain** processes through `RequestIdFilter` (MDC request ID assignment) then `HttpMethodOverrideFilter` (X-HTTP-Method-Override header support)
3. **Sa-Token Interceptor** validates authentication token if `@SaCheckLogin` present
4. **RateLimit Aspect** checks rate limits before method execution
5. **Controller** extracts user ID from `StpUtil.getLoginIdAsLong()`, calls Service method
6. **Service** performs business logic, parameter validation, calls Manager methods within transactions
7. **Manager** constructs QueryWrappers or calls custom Mapper methods, returns entities/BOs
8. **Mapper** executes SQL (MyBatis-Plus generated or custom XML)
9. **Response** flows back through layers, converted to VO/DTO in Service, wrapped in `HttpResponseBody.ok()` in Controller

**State Management:**
- Authentication state stored in Redis via Sa-Token (token-name: `satoken`)
- Rate limiting state stored in Redis (via `RateLimitService`)
- No server-side session state for business data

## Key Abstractions

**DTO Naming Convention:**
- Purpose: Data transfer objects for request/response boundaries
- Pattern: Use entity name as prefix (e.g., `UserSaveDTO`, `SongPageDTO`, `UserUpdatePasswordDTO`)
- Locations:
  - Request DTOs: `dto/request/user/`, `dto/request/song/`
  - Response VOs: `dto/response/user/`, `dto/response/song/`, `dto/response/session/`
  - Internal BOs: `dto/bo/`
  - Param objects: `dto/param/`

**CRUD Method Naming:**
- Create: `save`
- Delete: `remove`
- Update: `update`
- Read single: `get`
- Read multiple: `list`
- Read paginated: `page`

**Exception Handling Strategy:**
- All exceptions caught by `GlobalExceptionHandler`
- Translated via `ExceptionTranslator.translate()` using Java switch expressions
- Returns appropriate HTTP status codes with consistent JSON response format
- Business exceptions return HTTP 200 with success=false in body

**Rate Limiting Pattern:**
- Annotation-driven: `@RateLimit(refillRate = "3/60", capacity = 3, limitType = LimitType.IP)`
- Aspect-oriented implementation in `RateLimitAspect`
- Supports IP-based, User-based, or BOTH limiting strategies

## Entry Points

**Application Entry Point:**
- Location: `src/main/java/top/enderliquid/audioflow/AudioFlowApplication.java`
- Triggers: Spring Boot startup with `@SpringBootApplication`
- Responsibilities: Component scanning, mapper scanning (`@MapperScan`), scheduling enablement

**HTTP Endpoints:**

| Endpoint | Method | Auth Required | Description |
|----------|--------|---------------|-------------|
| `/api/users` | POST | No | User registration |
| `/api/users/me` | GET | Yes | Get current user info |
| `/api/users/me/password` | PATCH | Yes | Change password |
| `/api/sessions` | POST | No | Login |
| `/api/sessions/current` | DELETE | Yes | Logout |
| `/api/songs` | GET | No | Paginated song search |
| `/api/songs/{id}` | GET | No | Get song info |
| `/api/songs/{id}/play` | GET | No | Redirect to play URL |
| `/api/songs/prepare` | POST | Yes | Prepare song upload |
| `/api/songs/complete` | POST | Yes | Complete song upload |
| `/api/songs/{id}` | DELETE | Yes | Delete song |
| `/api/songs/{id}` | PATCH | Yes | Update song info |
| `/api/songs/batch-prepare` | POST | Yes | Batch prepare upload |
| `/api/songs/batch-complete` | POST | Yes | Batch complete upload |
| `/api/songs/batch` | POST | Yes | Batch delete songs |

**Scheduled Tasks:**
- `SongUploadCleanupTask.cleanupExpiredUploads()`: Runs hourly to clean up incomplete uploads older than 1 hour
- `LogCleanupRunner`: Manages log file cleanup on application startup

## Error Handling

**Strategy:** Centralized exception translation with typed responses

**Patterns:**
1. **Business Logic Errors**: Throw `BusinessException` -> translated to HTTP 200 with error message
2. **Authentication Errors**: Sa-Token exceptions -> translated to HTTP 401/403
3. **Validation Errors**: Jakarta Validation exceptions -> translated to HTTP 400 with field details
4. **System Errors**: Uncaught exceptions -> translated to HTTP 500 with request ID for tracking

**Key Components:**
- `GlobalExceptionHandler`: Catches all exceptions via `@ExceptionHandler(Exception.class)`
- `ExceptionTranslator`: Maps exception types to HTTP status codes and messages
- `ExceptionTranslateResult`: Holds status code and message for response construction

## Cross-Cutting Concerns

**Logging:**
- Framework: SLF4J with Lombok `@Slf4j`
- Pattern: `[时间] [线程] [级别] [类名] : [消息]`
- Request tracking via MDC `requestId` field (set by `RequestIdFilter`)
- All Chinese log messages per project convention

**Validation:**
- Framework: Jakarta Bean Validation (formerly javax.validation)
- Trigger points:
  - Controller: `@Valid @RequestBody` for JSON payloads
  - Controller: `@Valid @ModelAttribute` for form/query params
  - Service interface: `@Validated` at class level for method param validation
- Custom trimming: Empty strings converted to null via `JacksonConfig` (JSON) and `GlobalBindingAdvice` (form data)

**Authentication:**
- Framework: Sa-Token 1.44.0
- Token storage: Redis
- Token style: UUID
- Session duration: 7 days (604800 seconds)
- Concurrent login: Enabled (`is-concurrent=true`)
- Cookie reading: Disabled (`is-read-cookie=false`) - token passed via header

**Authorization:**
- Role-based: `Role.USER`, `Role.ADMIN`
- Permission checks: Service layer implements role-aware logic (e.g., admins can delete any song, users only their own)

---

*Architecture analysis: 2026-03-02*
