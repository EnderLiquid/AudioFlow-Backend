# Testing Patterns

**Analysis Date:** 2026-03-02

## Test Framework

**Runner:** JUnit 5 (Jupiter) - via `spring-boot-starter-test`

**Assertion Library:** JUnit 5 Assertions + Hamcrest matchers

**Run Commands:**
```bash
# Run all tests
./mvnw test

# Run single test class
./mvnw test -Dtest=HttpMethodOverrideFilterTest

# Run single test method
./mvnw test -Dtest=HttpMethodOverrideFilterTest#shouldOverrideMethodWhenHeaderPresentInPost

# Build skipping tests
./mvnw clean package -DskipTests
```

## Test File Organization

**Location:** Co-located with source in `src/test/java/` following same package structure

**Naming:** `[ClassUnderTest]Test.java`

**Structure:**
```
src/test/java/top/enderliquid/audioflow/
├── benchmark/
│   └── Argon2PasswordEncoderBenchmarkTest.java
├── common/
│   ├── MockOSSConfig.java
│   ├── TestDataHelper.java
│   ├── config/
│   │   └── RateLimitAspectIntegrationTest.java
│   ├── filter/
│   │   └── HttpMethodOverrideFilterTest.java
│   └── util/
│       └── FractionTest.java
├── config/
│   └── BaseControllerTest.java
└── controller/
    ├── SessionControllerTest.java
    ├── SongControllerTest.java
    └── UserControllerTest.java
```

## Test Structure

### Unit Test Pattern
```java
package top.enderliquid.audioflow.common.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class FractionTest {

    @Test
    void shouldConstructFractionFromIntegers() {
        Fraction fraction = new Fraction(3, 60);
        assertEquals(0.05, fraction.toDouble(), 0.0001);
    }

    @ParameterizedTest
    @CsvSource({
            "3/60, 0.05",
            "5/1, 5.0"
    })
    void shouldParseFractionStringAndConvertToDouble(String fractionStr, double expected) {
        Fraction fraction = new Fraction(fractionStr);
        assertEquals(expected, fraction.toDouble(), 0.0001);
    }

    @Test
    void shouldThrowExceptionWhenDenominatorIsZeroFromIntegers() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new Fraction(3, 0)
        );
        assertEquals("分母不能为0", exception.getMessage());
    }
}
```

### Integration Test Pattern (Controllers)
```java
@SpringBootTest
@AutoConfigureMockMvc
@Import(MockOSSConfig.class)
class UserControllerTest extends BaseControllerTest {

    @Autowired
    protected TestDataHelper testDataHelper;

    @Autowired
    protected ObjectMapper objectMapper;

    @Test
    void shouldRegisterSuccessfullyWhenEmailNotExists() throws Exception {
        Map<String, String> requestDto = new HashMap<>();
        requestDto.put("name", nickname);
        requestDto.put("email", email);
        requestDto.put("password", password);
        String requestJson = objectMapper.writeValueAsString(requestDto);

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.user.email").value(email));
    }
}
```

## Mocking

**Framework:** Mockito (included in Spring Boot Test)

**Patterns:**

For unit tests with mocks:
```java
@BeforeEach
void setUp() {
    filter = new HttpMethodOverrideFilter();
    filterChain = mock(FilterChain.class);
    response = new MockHttpServletResponse();
}

@Test
void shouldOverrideMethodWhenHeaderPresentInPost() throws ServletException, IOException {
    MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/test");
    request.addHeader("X-HTTP-Method-Override", "PUT");

    filter.doFilterInternal(request, response, filterChain);

    ArgumentCaptor<HttpServletRequest> requestCaptor = ArgumentCaptor.forClass(HttpServletRequest.class);
    verify(filterChain).doFilter(requestCaptor.capture(), eq(response));
    assertEquals("PUT", requestCaptor.getValue().getMethod());
}
```

**What to Mock:**
- External services (S3/OSS storage)
- Filter chains in servlet filter tests
- Dependencies in pure unit tests

**What NOT to Mock:**
- Database (use test database)
- Redis (use test database index 1)
- Spring context components in integration tests

## Fixtures and Factories

**Test Data Helper:** `src/test/java/top/enderliquid/audioflow/common/TestDataHelper.java`

```java
@Component
public class TestDataHelper {

    public User createTestUser() {
        User user = new User();
        String uniqueId = UUID.randomUUID().toString().substring(0, 8);
        user.setName("test_user_" + uniqueId);
        user.setEmail("test_user_" + uniqueId + "@example.com");
        user.setPassword(passwordEncoder.encode("test_password_123"));
        userManager.save(user);
        return user;
    }

    public User createTestAdmin() {
        User admin = new User();
        // ... setup admin user
        admin.setRole(Role.ADMIN);
        userManager.save(admin);
        return admin;
    }

    public Song createTestSong(Long userId) {
        Song song = new Song();
        song.setName("Test Song");
        song.setUploaderId(userId);
        song.setStatus(SongStatus.NORMAL);
        songManager.save(song);
        return song;
    }

    public void cleanAll() {
        cleanDatabase();
        redisTemplate.getConnectionFactory().getConnection().serverCommands().flushDb();
    }
}
```

## Configuration

**Base Test Class:** `src/test/java/top/enderliquid/audioflow/config/BaseControllerTest.java`

```java
@SpringBootTest
@AutoConfigureMockMvc
@Import(MockOSSConfig.class)
public abstract class BaseControllerTest {
    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected TestDataHelper testDataHelper;

    @BeforeEach
    void clearSession() {
        testDataHelper.cleanAll();
    }
}
```

**Mock Configuration:** `src/test/java/top/enderliquid/audioflow/common/MockOSSConfig.java`

```java
@TestConfiguration
public class MockOSSConfig {

    @Bean
    @Primary
    public OSSManager mockOSSManager() {
        return new MockOSSManager();
    }

    public static class MockOSSManager implements OSSManager {
        private final Map<String, byte[]> fileStorage = new HashMap<>();

        @Override
        public String generatePresignedPutUrl(String fileName, String mimeType) {
            return "https://mock-test-s3.example.com/upload/" + fileName;
        }

        public void simulateUpload(String fileName) throws IOException {
            Path testAudioFile = Paths.get("src/test/resources/audio/test-song.mp3");
            byte[] audioData = Files.readAllBytes(testAudioFile);
            fileStorage.put(fileName, audioData);
        }
    }
}
```

**Test Properties:** `src/test/resources/application.properties`

```properties
# Database - separate test database
spring.datasource.url=jdbc:mysql://127.0.0.1:3306/audioflow_test?useSSL=false&serverTimezone=UTC
spring.datasource.username=root
spring.datasource.password=${DB_PASSWORD:hachimi}

# Redis - separate database index
spring.data.redis.database=1
spring.data.redis.password=${REDIS_PASSWORD:hachimi}

# Reduced file upload limits for testing
spring.servlet.multipart.max-file-size=10MB
spring.servlet.multipart.max-request-size=20MB

# Log levels - reduce test output
logging.level.root=WARN
logging.level.top.enderliquid.audioflow=WARN

# S3 - dummy credentials for mocked service
file.storage.s3.access-key=test-key
file.storage.s3.secret-key=test-secret
file.storage.s3.bucket-name=test-bucket
```

## Test Types

### Unit Tests
- Scope: Single class/method
- No Spring context
- Fast execution
- Example: `FractionTest.java`, `HttpMethodOverrideFilterTest.java`

### Integration Tests
- Scope: Full application stack
- Uses `@SpringBootTest`
- Real database and Redis connections
- Example: `UserControllerTest.java`, `SongControllerTest.java`

### Benchmark Tests
- Performance measurement tests
- Example: `Argon2PasswordEncoderBenchmarkTest.java`

## Common Patterns

### Authentication in Tests
```java
// Login and get cookie
MvcResult result = mockMvc.perform(post("/api/sessions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginJson))
        .andExpect(status().isOk())
        .andReturn();

String cookie = result.getResponse().getCookie("satoken").getValue();

// Use cookie for authenticated requests
mockMvc.perform(get("/api/users/me")
                .cookie(new MockCookie("satoken", cookie)))
        .andExpect(status().isOk());
```

### JSON Request Building
```java
Map<String, String> requestDto = new HashMap<>();
requestDto.put("email", email);
requestDto.put("password", password);
String requestJson = objectMapper.writeValueAsString(requestDto);

mockMvc.perform(post("/api/sessions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
        .andExpect(...);
```

### JSON Response Parsing
```java
String response = result.getResponse().getContentAsString();
JsonNode jsonNode = objectMapper.readTree(response);
Long songId = jsonNode.get("data").get("id").asLong();
```

### Error Testing
```java
@Test
void shouldReturnErrorWhenEmailAlreadyExists() throws Exception {
    User user = testDataHelper.createTestUser();
    String email = user.getEmail();
    // ... setup request ...

    mockMvc.perform(post("/api/users")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestJson))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.message").value(containsString("邮箱")));
}
```

## Coverage

**No enforced coverage target** configured.

**View Coverage:**
```bash
./mvnw test jacoco:report
# Report location: target/site/jacoco/index.html
```

---

*Testing analysis: 2026-03-02*
