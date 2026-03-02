# Technology Stack

**Analysis Date:** 2026-03-02

## Languages

**Primary:**
- Java 21 - All application code

**Secondary:**
- XML - MyBatis mapper configurations (`src/main/resources/mapper/*.xml`)
- Properties - Configuration files
- Lua - Redis rate limiting scripts (`src/main/resources/scripts/rate_limit.lua`)

## Runtime

**Environment:**
- JVM (Java Virtual Machine) 21
- GraalVM JDK 21 (per `.idea/misc.xml`)

**Package Manager:**
- Maven 3.9.11
- Lockfile: `pom.xml` with explicit versions

## Frameworks

**Core:**
- Spring Boot 3.5.10 - Main application framework
- Spring Web - REST API endpoints
- Spring Data Redis - Redis integration
- Spring Validation - Bean validation
- Spring AOP - Aspect-oriented programming
- Spring Boot Actuator - Health checks and metrics

**Persistence:**
- MyBatis-Plus 3.5.15 - ORM framework
- MySQL Connector/J - Database driver

**Authentication:**
- Sa-Token 1.44.0 - Session authentication framework
- Sa-Token Redis Template - Redis session storage

**Security:**
- Spring Security Crypto - Password encoding
- Bouncy Castle 1.83 - Cryptographic provider

**File Processing:**
- Apache Tika 3.2.3 - File type detection

**Object Storage:**
- AWS SDK for Java S3 2.29.45 - S3-compatible storage

**Build/Dev:**
- Spring Boot DevTools - Development utilities
- Lombok - Code generation
- Maven Compiler Plugin
- Spring Boot Maven Plugin

## Key Dependencies

**Critical:**
- `spring-boot-starter-web` - Web layer foundation
- `mybatis-plus-spring-boot3-starter` 3.5.15 - Data access layer
- `sa-token-spring-boot3-starter` 1.44.0 - Authentication
- `software.amazon.awssdk:s3` 2.29.45 - File storage

**Infrastructure:**
- `spring-boot-starter-data-redis` - Caching and session storage
- `apache.commons:commons-pool2` - Connection pooling
- `mysql-connector-j` - Database connectivity

**Utilities:**
- `lombok` - Boilerplate reduction
- `jackson` (transitive) - JSON serialization
- `jakarta.validation-api` - Validation annotations

## Configuration

**Environment:**
- Multi-profile configuration: `dev`, `prod`, `test`
- Priority (high to low):
  1. Command line arguments
  2. Environment variables
  3. External config: `config/application-{env}.properties`
  4. Internal config: `src/main/resources/application-{env}.properties`
  5. Base config: `src/main/resources/application.properties`

**Key Config Files:**
- `src/main/resources/application.properties` - Base configuration
- `src/main/resources/application-dev.properties` - Development settings
- `src/main/resources/application-prod.properties` - Production settings
- `src/test/resources/application.properties` - Test configuration
- `config/` directory (gitignored) - External sensitive config

**Build:**
- `pom.xml` - Maven build configuration
- `.mvn/wrapper/maven-wrapper.properties` - Wrapper settings

**Logging:**
- `src/main/resources/logback-spring.xml` - Logback configuration
- Console and file appenders
- Request ID tracking via MDC

## Platform Requirements

**Development:**
- Java 21 (GraalVM recommended)
- Maven 3.9+ or use wrapper (`./mvnw`)
- MySQL 8.0+
- Redis 6.0+
- Port 8081 (default dev port)

**Production:**
- Java 21 runtime
- MySQL database server
- Redis server for sessions and caching
- S3-compatible object storage (AWS S3, OSS, COS, MinIO, Qiniu)
- Port 80 (default prod port)

---

*Stack analysis: 2026-03-02*
