# AGENTS.md - AudioFlow Agent Guidelines

## Project Overview

AudioFlow is a Java 21 Spring Boot application with Maven build system.

## Build Commands

```bash
# Compile the project
./mvnw clean compile

# Build the project
./mvnw clean package

# Run the application
./mvnw spring-boot:run

# Run all tests
./mvnw test

# Run a single test class
./mvnw test -Dtest=HttpMethodOverrideFilterTest

# Run a single test method
./mvnw test -Dtest=HttpMethodOverrideFilterTest#shouldOverrideMethodWhenHeaderPresentInPost

# Skip tests during build
./mvnw clean package -DskipTests
```

## Project Architecture

Four-layer architecture (top to bottom):
- **Controller**: REST API endpoints
- **Service**: Business logic layer
- **Manager**: Data access layer (extends MyBatis-Plus IService)
- **Mapper**: Database mapping

### Architecture Rules
1. No layer skipping (Controller cannot access Manager directly)
2. No bottom-to-top access
3. Manager extends MyBatis-Plus interfaces
4. Service cannot use QueryWrapper (conditions built in Manager)
5. Service cannot create Page objects (built in Manager)
6. All parameter validation in Service layer

## Code Style Guidelines

### Naming Conventions
- **Classes**: PascalCase (e.g., `UserController`, `UserService`)
- **Methods**: camelCase with semantic meaning
- **DTO naming**: Entity name prefix (e.g., `UserSaveDTO`, `SongUpdateDTO`)
- **CRUD naming**: `save` (create), `remove` (delete), `update` (modify), `get` (single), `list` (multiple), `page` (pagination)

### Annotations
- All Controllers: `@Validated` at class level
- All Service interfaces: `@Validated` at class level
- DTO parameters: `@Valid` for validation
- Logging: `@Slf4j` (Lombok)

### API Design
- **HTTP Methods**: Only GET and POST in production
- **Method Override**: Use `X-HTTP-Method-Override` header for PUT/PATCH/DELETE semantics on POST
- **Resource paths**: Plural nouns, lowercase, hyphen-separated (e.g., `/api/users`, `/api/songs`)
- **Path parameters**: Use `{id}` for resource identification
- **Sessions**: Login/logout at `/api/sessions`

### DTO Guidelines
1. String auto-trim enabled via `JacksonConfig` and `GlobalBindingAdvice`
2. Default values logic in Service layer, keep DTOs pure
3. Naming: Entity prefix (e.g., `UserSaveDTO`, `SongPageDTO`)

### Error Handling
- Throw `BusinessException` for business failures
- Use `HttpResponseBody.ok()` for success
- Use `HttpResponseBody.fail()` for failures
- Global exception handler in `GlobalExceptionHandler`

### Language Requirements
- **All code comments must be in Chinese**
- **All log messages must be in Chinese**
- **All exception messages must be in Chinese**
- **All git commit messages must be in Chinese**

### Logging Format
- Entry: `请求XXX，参数名: {}`
- Success: `XXX成功` or `XXX成功，关键信息: {}`
- Use `warn`/`error` for unexpected failures
- Use `BusinessException` for expected business failures

## Technology Stack

- **Java**: 21
- **Spring Boot**: 3.5.10
- **Auth**: Sa-Token
- **Persistence**: MyBatis-Plus
- **Validation**: Jakarta Validation
- **Utilities**: Lombok, Apache Tika, AWS S3 SDK

## Testing

- Framework: JUnit 5
- Location: `src/test/java`
- Use Spring's `MockHttpServletRequest` for servlet testing
- Mockito for mocking dependencies
- Test methods should be descriptive (e.g., `shouldOverrideMethodWhenHeaderPresentInPost`)

## Additional Resources

See `DEVELOPMENT.md` for detailed development guidelines in Chinese.
