# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build Commands

This is a Maven-based Spring Boot project using Java 21.

```bash
# Compile
./mvnw clean compile

# Build package
./mvnw clean package

# Run application (dev environment)
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

# Run all tests
./mvnw test

# Run single test class
./mvnw test -Dtest=HttpMethodOverrideFilterTest

# Run single test method
./mvnw test -Dtest=HttpMethodOverrideFilterTest#shouldOverrideMethodWhenHeaderPresentInPost

# Build skipping tests
./mvnw clean package -DskipTests
```

## Architecture Overview

### Four-Layer Architecture (top-down)

1. **Controller**: REST API endpoints, request/response handling
2. **Service**: Business logic, parameter validation, transaction management
3. **Manager**: Data access layer - extends MyBatis-Plus `IService<Entity>` and `ServiceImpl<Mapper, Entity>`
4. **Mapper**: Database mapping layer (MyBatis-Plus interfaces)

### Layer Access Rules

- **No cross-layer access**: Controller cannot directly access Manager
- **No bottom-up access**: Lower layers cannot access upper layers
- **Manager responsibility**: All QueryWrapper construction and Page object creation happen in Manager layer
- **Service responsibility**: All parameter validation happens in Service layer

## Code Style Requirements

### Language

- **All code comments must be in Chinese**
- **All log messages must be in Chinese**
- **All exception messages must be in Chinese**
- **All Git commit messages must be in Chinese**
- **No conventional commit prefixes**: Do not use `feat:`, `fix:`, `docs:` etc. Use plain Chinese descriptions

### Type Declarations

- **Never use `var` keyword**: All variables must use explicit type declarations
- This applies to: local variables, for-each loops, try-with-resources, lambda parameters

### Naming Conventions

- **DTO naming**: Use entity name as prefix (e.g., `UserSaveDTO`, `SongPageDTO`, `UserUpdatePasswordDTO`)
- **CRUD methods**:
  - Create: `save`
  - Delete: `remove`
  - Update: `update`
  - Read single: `get`
  - Read multiple: `list`
  - Read paginated: `page`

### Annotations

- All Controller classes: `@Validated` at class level
- All Service interfaces: `@Validated` at class level
- DTO parameters: `@Valid` for validation
- Logging: Use Lombok `@Slf4j`

## API Design Patterns

### HTTP Methods

- Production only uses `GET` and `POST`
- For PUT/PATCH/DELETE semantics, use `POST` with `X-HTTP-Method-Override` header
- Resource paths use plural nouns, lowercase, hyphen-separated (e.g., `/api/users`, `/api/songs`)
- Session management at `/api/sessions`

### Response Format

All responses use `HttpResponseBody<T>`:
- Success: `HttpResponseBody.ok(data)` or `HttpResponseBody.ok(data, "message")`
- Failure: `HttpResponseBody.fail("message")`
- Business failures throw `BusinessException`

### Logging Pattern (Service Layer)

```java
// Entry
log.info("请求XXX，参数名: {}", param);

// Success exit
log.info("XXX成功");
// or
log.info("XXX成功，关键信息: {}", info);

// Business failures - throw exception
throw new BusinessException("失败原因");
```

## DTO Handling

- String fields are automatically trimmed via `JacksonConfig` (JSON) and `GlobalBindingAdvice` (form data)
- Empty strings become null after trimming
- Default value logic belongs in Service layer, not DTO

## Configuration Structure

Multi-environment configuration with priority (high to low):
1. Command line arguments
2. Environment variables
3. External config: `config/application-{env}.properties`
4. Internal config: `src/main/resources/application-{env}.properties`
5. Base config: `src/main/resources/application.properties`

The `config/` directory is gitignored for sensitive data.

## Testing

- Test resources are isolated in `src/test/resources/application.properties`
- Tests use a separate database (`audioflow_test`) and Redis database (1)
- File storage uses local mode in tests (not S3)
- Log level is WARN in tests to reduce output

## Technology Stack

- **Java**: 21
- **Spring Boot**: 3.5.10
- **Persistence**: MyBatis-Plus 3.5.15
- **Authentication**: Sa-Token 1.44.0
- **Password Encoding**: Argon2 (primary), BCrypt (fallback)
- **File Storage**: AWS S3 SDK (configurable for OSS/COS/MinIO)
- **File Type Detection**: Apache Tika
- **Redis**: Spring Data Redis with Lettuce
