# Coding Conventions

**Analysis Date:** 2026-03-02

## Language Requirements

**All code content must be in Chinese:**
- All code comments must be in Chinese
- All log messages must be in Chinese
- All exception messages must be in Chinese
- All Git commit messages must be in Chinese
- No conventional commit prefixes (do not use `feat:`, `fix:`, `docs:` etc.)

Example from `UserServiceImpl.java`:
```java
log.info("请求注册普通用户，邮箱: {}", dto.getEmail());
throw new BusinessException("邮箱已被注册");
```

## Type Declarations

**Never use `var` keyword.** All variables must use explicit type declarations.

Applies to:
- Local variables
- For-each loops
- Try-with-resources
- Lambda parameters

Example:
```java
// Correct
String email = user.getEmail();
List<SongVO> songVOList = new ArrayList<>();

// Incorrect - do not use
var email = user.getEmail();
```

## Naming Conventions

### Files
- **Controller classes**: `{Entity}Controller.java` (e.g., `UserController.java`, `SongController.java`)
- **Service interfaces**: `{Entity}Service.java` (e.g., `UserService.java`)
- **Service implementations**: `{Entity}ServiceImpl.java` (e.g., `UserServiceImpl.java`)
- **Manager interfaces**: `{Entity}Manager.java` (e.g., `UserManager.java`)
- **Manager implementations**: `{Entity}ManagerImpl.java` (e.g., `UserManagerImpl.java`)
- **Mapper interfaces**: `{Entity}Mapper.java` (e.g., `UserMapper.java`)

### DTO Naming
Use entity name as prefix:
- Create: `{Entity}SaveDTO` (e.g., `UserSaveDTO`, `SongPrepareUploadDTO`)
- Update: `{Entity}UpdateDTO` (e.g., `UserUpdatePasswordDTO`, `SongUpdateDTO`)
- Query/Page: `{Entity}PageDTO` (e.g., `SongPageDTO`)
- Verify/Other: `{Entity}{Action}DTO` (e.g., `UserVerifyPasswordDTO`)

### CRUD Methods
| Operation | Method Name |
|-----------|-------------|
| Create | `save` |
| Delete | `remove` |
| Update | `update` |
| Read single | `get` |
| Read multiple | `list` |
| Read paginated | `page` |

Examples from codebase:
```java
// UserService.java
UserVO saveUser(UserSaveDTO dto);
UserVO getUser(Long userId);
UserVO updateUserPassword(UserUpdatePasswordDTO dto, Long userId);

// SongService.java
void removeSong(Long songId, Long userId);
PageVO<SongVO> pageSongsByUploaderKeywordAndSongKeyword(SongPageDTO dto);
```

### Variables and Parameters
- Use camelCase for variables and parameters
- Use descriptive names in Chinese context
- Boolean fields: avoid `is` prefix (Lombok handles this)

## Annotations

### Controller Classes
```java
@RestController
@RequestMapping("/api/users")
@Validated  // Required at class level
public class UserController {
    // ...
}
```

### Service Interfaces
```java
@Validated  // Required at interface level
public interface UserService {
    // ...
}
```

### DTO Validation
```java
@Data
public class UserSaveDTO {
    @NotBlank(message = "用户名不能为空")
    @Size(min = 1, max = 64, message = "用户名长度必须在1-64个字符之间")
    private String name;
}
```

### Logging
Always use Lombok `@Slf4j`:
```java
@Slf4j
@Service
public class UserServiceImpl implements UserService {
    // Use log.info(), log.warn(), log.error()
}
```

## Code Style

### Formatting
- Standard Java conventions
- Indent: 4 spaces
- Max line length: ~120 characters
- Braces: same line for opening brace

### Import Organization
Order (observed in codebase):
1. Java standard library (`java.*`, `javax.*`)
2. Spring framework (`org.springframework.*`)
3. Third-party libraries (Lombok, MyBatis-Plus, Sa-Token, etc.)
4. Project internal imports (`top.enderliquid.audioflow.*`)

Example from `SongController.java`:
```java
import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import top.enderliquid.audioflow.common.annotation.RateLimit;
```

## Error Handling

### Business Exceptions
Use `BusinessException` for business logic failures:
```java
if (user == null) {
    throw new BusinessException("用户不存在");
}
if (!passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
    throw new BusinessException("密码错误");
}
```

### Exception Translation
All exceptions are translated via `ExceptionTranslator` and handled by `GlobalExceptionHandler`.

Do NOT manually catch and wrap exceptions in controllers/services unless special handling is needed.

## Logging Pattern (Service Layer)

### Entry
```java
log.info("请求XXX，参数名: {}", param);
```

### Success Exit
```java
log.info("XXX成功");
// or
log.info("XXX成功，关键信息: {}", info);
```

### Business Failures
Throw exception instead of logging error:
```java
throw new BusinessException("失败原因");
```

Example from `UserServiceImpl.java`:
```java
@Override
public UserVO saveUser(UserSaveDTO dto) {
    log.info("请求注册普通用户，邮箱: {}", dto.getEmail());
    // ... business logic ...
    log.info("注册用户成功，用户ID: {}", user.getId());
    return userVO;
}
```

## API Design Patterns

### HTTP Methods
Production only uses `GET` and `POST`. For PUT/PATCH/DELETE semantics, use `POST` with `X-HTTP-Method-Override` header.

### Resource Paths
- Use plural nouns
- Lowercase
- Hyphen-separated for multi-word endpoints

Examples:
```
/api/users              # User resource
/api/songs              # Song resource
/api/sessions           # Session management
/api/songs/prepare      # Upload preparation
/api/songs/batch-prepare # Batch operations
```

### Response Format
All responses use `HttpResponseBody<T>`:
```java
// Success
return HttpResponseBody.ok(data);
return HttpResponseBody.ok(data, "操作成功");

// Failure - throw exception
throw new BusinessException("操作失败");
```

## DTO Handling

### String Trimming
String fields are automatically trimmed:
- JSON requests: via `JacksonConfig`
- Form data: via `GlobalBindingAdvice`
- Empty strings become `null` after trimming

### Default Values
Default value logic belongs in Service layer, NOT in DTO:
```java
// In Service layer
if (param.getIsAsc() == null) param.setIsAsc(false);
if (param.getPageIndex() == null) param.setPageIndex(1L);
if (param.getPageSize() == null) param.setPageSize(10L);
```

## Function Design

### Size Guidelines
- Keep methods focused on single responsibility
- Extract complex logic into private methods
- Example: `SongServiceImpl.completeUpload()` delegates to `getAudioDurationInMills()`

### Parameter Validation
- Use Bean Validation annotations (`@NotNull`, `@NotBlank`, `@Size`, `@Email`) on DTOs
- Additional validation logic in Service layer

### Return Values
- Service interfaces use `@Nullable` for nullable returns
- Controllers always return `HttpResponseBody<T>`

## Module Design

### Layer Access Rules
- **No cross-layer access**: Controller cannot directly access Manager
- **No bottom-up access**: Lower layers cannot access upper layers
- **Manager responsibility**: All QueryWrapper construction and Page object creation happen in Manager layer
- **Service responsibility**: All parameter validation happens in Service layer

### Four-Layer Architecture
1. **Controller**: REST API endpoints, request/response handling
2. **Service**: Business logic, parameter validation, transaction management
3. **Manager**: Data access layer (extends MyBatis-Plus `IService<Entity>`)
4. **Mapper**: Database mapping layer (MyBatis-Plus interfaces)

---

*Convention analysis: 2026-03-02*
