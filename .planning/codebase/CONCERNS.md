# Codebase Concerns

**Analysis Date:** 2026-03-02

## Tech Debt

### S3 Client Null Check Pattern
- Issue: `OSSManagerImpl` returns `null` for all operations when S3 credentials are not configured, requiring callers to handle null returns
- Files: `src/main/java/top/enderliquid/audioflow/manager/impl/OSSManagerImpl.java`
- Impact: Silent failures - methods return null instead of throwing exceptions, making debugging difficult
- Fix approach: Throw a custom exception (e.g., `StorageNotConfiguredException`) or use Optional return types

### Transaction Template Return Null Pattern
- Issue: `UserServiceImpl.doSaveUser()` uses `TransactionTemplate.execute()` with lambda returning `null`
- Files: `src/main/java/top/enderliquid/audioflow/service/impl/UserServiceImpl.java` (line 60)
- Impact: Code smell - using transaction template purely for side effects without meaningful return
- Fix approach: Consider using `@Transactional` annotation instead, or use `TransactionCallbackWithoutResult`

### Rate Limit Key Generation Bug
- Issue: `RateLimitServiceImpl.generateUserIdKey()` incorrectly prefixes with "ip-" instead of "user-"
- Files: `src/main/java/top/enderliquid/audioflow/service/impl/RateLimitServiceImpl.java` (line 63)
- Impact: User-based rate limits share keys with IP-based limits, causing incorrect rate limiting behavior
- Fix approach: Change `"ip-" + userId` to `"user-" + userId`

## Known Bugs

### Fraction Class Missing Validation Edge Case
- Issue: `Fraction` constructor accepts strings like "1/-2" which creates negative denominator
- Files: `src/main/java/top/enderliquid/audioflow/common/util/Fraction.java`
- Trigger: Pass "1/-2" to constructor
- Current behavior: Creates fraction with negative denominator
- Expected: Should normalize or reject negative denominators

### Song Upload Cleanup Task Race Condition
- Issue: `SongUploadCleanupTask.cleanupExpiredUploads()` checks file existence before deletion, but file could be uploaded between check and delete
- Files: `src/main/java/top/enderliquid/audioflow/common/task/SongUploadCleanupTask.java`
- Trigger: User uploads file exactly at cleanup time
- Impact: Valid upload might be incorrectly deleted
- Workaround: None currently

### Get Song URL Returns Null Silently
- Issue: `SongServiceImpl.getSongUrl()` returns `null` when song doesn't exist instead of throwing exception
- Files: `src/main/java/top/enderliquid/audioflow/service/impl/SongServiceImpl.java` (lines 298-299)
- Impact: Inconsistent error handling - other methods throw `BusinessException` for missing resources
- Fix approach: Throw `BusinessException("歌曲不存在")` instead of returning null

## Security Considerations

### Exception Detail Exposure Configuration
- Risk: `response.exception.expose-uncaught-exception-detail=true` in production exposes internal error details
- Files: `src/main/resources/application.properties` (line 62)
- Current mitigation: Configurable via property, defaults to true in dev
- Recommendations: Set default to false, override only in development profiles

### CORS Allowed Origins Wildcard in Test
- Risk: Test configuration allows all origins (`app.cors.allowed-origin-patterns=*`)
- Files: `src/test/resources/application.properties` (line 25)
- Current mitigation: Only in test environment
- Recommendations: Ensure production config restricts origins appropriately

### Password in Test Configuration
- Risk: Default database password exposed in test properties
- Files: `src/test/resources/application.properties` (line 7)
- Current mitigation: Uses placeholder `${DB_PASSWORD:hachimi}` with fallback
- Recommendations: Remove fallback value, require explicit environment variable

### File Type Detection Bypass Potential
- Issue: MIME type whitelist in `SongServiceImpl` relies on Apache Tika detection which can be bypassed
- Files: `src/main/java/top/enderliquid/audioflow/service/impl/SongServiceImpl.java` (lines 52-69)
- Risk: Malicious files with spoofed headers might pass validation
- Recommendations: Add additional file content validation beyond MIME type

## Performance Bottlenecks

### Audio Duration Parsing Blocks Thread
- Issue: `getAudioDurationInMills()` parses audio metadata synchronously on the calling thread
- Files: `src/main/java/top/enderliquid/audioflow/service/impl/SongServiceImpl.java` (lines 198-220)
- Cause: Apache Tika parser runs synchronously during `completeUpload()`
- Improvement path: Move duration parsing to async task or background job

### Redis Lua Script Loading on Every Call
- Issue: `RedisManagerImpl` loads Lua script from classpath on every rate limit check
- Files: `src/main/java/top/enderliquid/audioflow/manager/impl/RedisManagerImpl.java`
- Cause: Script is loaded in `@PostConstruct` but could be cached more efficiently
- Note: Currently correctly cached in instance variable after initialization

### Database Query N+1 Risk in Song Listing
- Issue: `pageSongsByUploaderKeywordAndSongKeyword` joins user table but individual uploader lookups may still occur
- Files: `src/main/java/top/enderliquid/audioflow/service/impl/SongServiceImpl.java` (lines 222-248)
- Current state: Uses JOIN in mapper XML, appears optimized
- Monitoring: Watch for additional queries in logs

## Fragile Areas

### Snowflake ID Converter Silent Failures
- Issue: `SnowflakeIdConverter.fromString()` returns `null` for any invalid input without logging
- Files: `src/main/java/top/enderliquid/audioflow/common/util/id/SnowflakeIdConverter.java`
- Why fragile: Makes debugging search parameter issues difficult
- Safe modification: Add debug logging before returning null
- Test coverage: Unit tests exist in codebase

### OSS Manager Initialization Failure Handling
- Issue: If S3 credentials are missing, `OSSManagerImpl` logs warning and continues with null clients
- Files: `src/main/java/top/enderliquid/audioflow/manager/impl/OSSManagerImpl.java` (lines 55-60)
- Why fragile: Application starts successfully but all file operations fail silently
- Safe modification: Make S3 configuration required or add health check indicator
- Test coverage: Mock implementation exists for tests

### Batch Operation Exception Handling
- Issue: Batch operations stop processing on first non-BusinessException
- Files: `src/main/java/top/enderliquid/audioflow/service/impl/SongServiceImpl.java` (lines 340-402)
- Why fragile: System exceptions (like OutOfMemoryError) halt entire batch
- Safe modification: Catch Throwable and log, continue with remaining items
- Test coverage: Integration tests exist

### Log File Cleanup Logic
- Issue: `LogCleanupRunner` uses string pattern matching for log files that depends on naming convention
- Files: `src/main/java/top/enderliquid/audioflow/common/task/LogCleanupRunner.java` (line 30)
- Why fragile: Changing logback-spring.xml pattern breaks cleanup
- Safe modification: Use file attributes or logback API instead of pattern matching
- Test coverage: No specific unit tests found

## Scaling Limits

### Rate Limiting Redis Single Point
- Resource: Redis-based rate limiting
- Current capacity: Depends on Redis performance and network latency
- Limit: All rate limit checks hit Redis; Redis failure causes rate limiting to fail-open (allows all requests)
- Scaling path: Consider local caching for rate limits or Redis Cluster

### File Storage S3 Bottleneck
- Resource: S3-compatible object storage
- Current capacity: Limited by S3 provider's rate limits
- Limit: Pre-signed URL generation and file existence checks are synchronous
- Scaling path: Implement connection pooling and circuit breaker pattern

### Session Storage Redis Database
- Resource: Sa-Token session storage in Redis
- Current capacity: Single Redis instance/database
- Limit: Session data grows unbounded as users increase
- Scaling path: Configure session expiration policies and Redis memory limits

## Dependencies at Risk

### Sa-Token Version Compatibility
- Package: `cn.dev33:sa-token-spring-boot3-starter:1.44.0`
- Risk: Using older version; potential security fixes in newer versions
- Impact: Authentication/session management
- Migration plan: Monitor Sa-Token releases for security advisories

### MyBatis-Plus SQL Parser Dependency
- Package: `mybatis-plus-jsqlparser:3.5.15`
- Risk: JSqlParser has had CVEs in the past
- Impact: SQL injection if parser fails
- Migration plan: Keep updated, monitor security advisories

### Bouncy Castle Provider
- Package: `bcprov-jdk18on:1.83`
- Risk: Cryptographic provider - critical for password hashing
- Impact: Password security
- Migration plan: Regular updates for security patches

## Missing Critical Features

### Health Check Endpoints
- Problem: No Spring Boot Actuator health indicators for external dependencies (Redis, S3)
- Blocks: Production monitoring and Kubernetes readiness/liveness probes
- Priority: High

### Request/Response Logging
- Problem: No middleware/filter for logging request/response bodies for debugging
- Blocks: Troubleshooting API issues in production
- Priority: Medium

### API Documentation
- Problem: No OpenAPI/Swagger documentation generated
- Blocks: Frontend developers and API consumers
- Priority: Medium

### Admin User Creation Endpoint
- Problem: `UserServiceImpl.saveAdminUser()` exists but no controller endpoint exposes it
- Blocks: Cannot create admin users via API (only direct DB insertion)
- Priority: Low (may be intentional for security)

## Test Coverage Gaps

### Rate Limit Service Edge Cases
- What's not tested: Redis failure scenarios, fractional refill rates boundary conditions
- Files: `src/main/java/top/enderliquid/audioflow/service/impl/RateLimitServiceImpl.java`
- Risk: Rate limiting bugs only appear under load or Redis issues
- Priority: Medium

### OSS Manager Error Handling
- What's not tested: S3 SDK exception handling, network timeout scenarios
- Files: `src/main/java/top/enderliquid/audioflow/manager/impl/OSSManagerImpl.java`
- Risk: File operation failures not properly handled
- Priority: High

### Log Cleanup Task
- What's not tested: Log file cleanup logic, size calculation edge cases
- Files: `src/main/java/top/enderliquid/audioflow/common/task/LogCleanupRunner.java`
- Risk: Logs may not be cleaned properly, filling disk space
- Priority: Medium

### Batch Operations Rollback
- What's not tested: Partial batch failure scenarios, data consistency after failures
- Files: `src/main/java/top/enderliquid/audioflow/service/impl/SongServiceImpl.java` (batch methods)
- Risk: Data inconsistency in partial failures
- Priority: Medium

---

*Concerns audit: 2026-03-02*
