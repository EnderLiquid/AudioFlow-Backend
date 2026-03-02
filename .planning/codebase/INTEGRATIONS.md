# External Integrations

**Analysis Date:** 2026-03-02

## APIs & External Services

**Object Storage (S3-Compatible):**
- Service: AWS S3 SDK with configurable endpoints
- Purpose: File upload/download/storage for audio files
- Implementation: `top.enderliquid.audioflow.manager.impl.OSSManagerImpl`
- Supported Providers:
  - AWS S3
  - Alibaba Cloud OSS
  - Tencent Cloud COS
  - Qiniu Cloud
  - MinIO (self-hosted)
- Features:
  - Presigned URL generation for direct browser uploads
  - File existence checking
  - File streaming download
  - File deletion
  - Configurable path-style vs virtual-hosted style access

## Data Storage

**Primary Database:**
- Type: MySQL 8.0+
- Driver: `com.mysql.cj.jdbc.Driver`
- ORM: MyBatis-Plus 3.5.15
- Connection: JDBC with connection pooling
- Entities:
  - `User` - User accounts and authentication
  - `Song` - Audio file metadata
- Custom Mappers:
  - `SongMapper.selectPageByUploaderInfoOrSongInfo` - Complex pagination query with joins

**Caching & Session Storage:**
- Type: Redis
- Client: Spring Data Redis with Lettuce
- Use Cases:
  - Sa-Token session storage
  - Rate limiting token bucket implementation
  - Distributed locking (if needed)
- Lua Scripts:
  - `scripts/rate_limit.lua` - Token bucket rate limiting algorithm

**File Storage:**
- Type: S3-compatible object storage
- Local Fallback: Not implemented (requires valid S3 credentials)
- Max Upload Size: 20MB (configurable via `spring.servlet.multipart.max-file-size`)

## Authentication & Identity

**Auth Provider:**
- Framework: Sa-Token 1.44.0
- Type: Session-based token authentication
- Token Style: UUID
- Storage: Redis (via `sa-token-redis-template`)
- Session Duration: 7 days (604800 seconds)
- Features:
  - Concurrent login support
  - Non-shared tokens per session
  - Cookie-less token reading (header-based)

**Password Encryption:**
- Primary: Argon2
- Fallback: BCrypt
- Configuration:
  - Argon2 parallelism: 1
  - Argon2 memory: 16384 KB
  - Argon2 iterations: 3
  - BCrypt work factor: 12

**Authorization:**
- Role-based access control (RBAC)
- Roles: Defined in `top.enderliquid.audioflow.common.enums.Role`
- Permission interface: `StpInterfaceImpl`

## Monitoring & Observability

**Health Checks:**
- Endpoint: Spring Boot Actuator
- Default path: `/actuator/health`

**Logging:**
- Framework: SLF4J with Logback
- Output: Console + File
- File Location: `./logs/AudioFlow-{timestamp}.log`
- Features:
  - Request ID tracking (MDC)
  - Automatic log rotation based on total size cap (100MB default)
  - Chinese log messages (per project convention)

**Error Tracking:**
- Not integrated (no Sentry, Bugsnag, etc.)
- Exception handling: `GlobalExceptionHandler`

## CI/CD & Deployment

**Hosting:**
- Not specified (generic JAR deployment)
- Packaging: Executable Spring Boot JAR

**CI Pipeline:**
- Not detected

## Environment Configuration

**Required Environment Variables:**
- `DB_PASSWORD` - MySQL database password
- `REDIS_PASSWORD` - Redis server password
- `S3_ACCESS_KEY` - S3 access key
- `S3_SECRET_KEY` - S3 secret key

**Optional Environment Variables:**
- `worker.id` - Snowflake ID worker ID
- `datacenter.id` - Snowflake ID datacenter ID

**Secrets Location:**
- Environment variables (preferred for production)
- External config files in `config/` directory (gitignored)
- NOT committed to repository

## Webhooks & Callbacks

**Incoming:**
- None detected

**Outgoing:**
- None detected

## Network Configuration

**CORS:**
- Configured via `WebMvcConfig`
- Allowed methods: GET, POST, PUT, DELETE, OPTIONS
- Credentials allowed
- Origin patterns configurable per environment

**HTTP Method Override:**
- Filter: `HttpMethodOverrideFilter`
- Header: `X-HTTP-Method-Override`
- Purpose: Support PUT/PATCH/DELETE semantics over POST (production constraint)

---

*Integration audit: 2026-03-02*
