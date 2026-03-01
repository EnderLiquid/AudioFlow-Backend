# 日志文件输出功能实现计划

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 实现日志输出到文件功能，每次启动生成独立日志文件，限制日志文件总大小。

**Architecture:** 使用 logback 的 `<timestamp>` 元素生成启动时间戳作为文件名，配合 Spring Boot ApplicationRunner 在启动时清理旧日志文件，确保总大小不超过限制。

**Tech Stack:** Spring Boot 3.5.10, Logback, Java 21

---

### Task 1: 修改 logback-spring.xml 配置

**Files:**
- Modify: `src/main/resources/logback-spring.xml`

**Step 1: 更新 logback 配置文件**

将现有配置修改为：

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <!-- 启动时间戳，用于生成独立日志文件 -->
    <timestamp key="startupTime" datePattern="yyyy-MM-dd_HH-mm-ss"/>

    <!-- 控制台输出 -->
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss} [%thread] [%X{requestId}] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>

    <!-- 文件输出 -->
    <appender name="FILE" class="ch.qos.logback.core.FileAppender">
        <file>./logs/AudioFlow-${startupTime}.log</file>
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss} [%thread] [%X{requestId}] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>

    <root level="INFO">
        <appender-ref ref="CONSOLE"/>
        <appender-ref ref="FILE"/>
    </root>
</configuration>
```

**Step 2: 验证配置语法**

确保 XML 格式正确，无语法错误。

---

### Task 2: 添加日志配置属性

**Files:**
- Modify: `src/main/resources/application.properties`

**Step 1: 添加日志大小限制配置**

在文件末尾添加：

```properties
############## 日志配置 ##############
# 日志文件总大小限制（支持KB/MB/GB后缀，默认100MB）
logging.total-size-cap=100MB
```

---

### Task 3: 创建日志清理组件

**Files:**
- Create: `src/main/java/top/enderliquid/audioflow/common/task/LogCleanupRunner.java`

**Step 1: 创建 LogCleanupRunner 类**

```java
package top.enderliquid.audioflow.common.task;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Component
public class LogCleanupRunner implements ApplicationRunner {

    private static final String LOG_DIR = "./logs";
    private static final Pattern LOG_FILE_PATTERN = Pattern.compile("^AudioFlow-\\d{4}-\\d{2}-\\d{2}_\\d{2}-\\d{2}-\\d{2}\\.log$");

    @Value("${logging.total-size-cap:100MB}")
    private String totalSizeCap;

    @Override
    public void run(ApplicationArguments args) {
        log.info("开始检查日志文件大小限制");
        try {
            cleanupOldLogs();
        } catch (Exception e) {
            log.warn("清理旧日志文件失败", e);
        }
    }

    private void cleanupOldLogs() throws IOException {
        Path logDir = Paths.get(LOG_DIR);
        if (!Files.exists(logDir)) {
            log.info("日志目录不存在，跳过清理");
            return;
        }

        List<LogFileInfo> logFiles = findLogFiles(logDir);
        if (logFiles.isEmpty()) {
            log.info("没有找到日志文件，跳过清理");
            return;
        }

        long totalSize = logFiles.stream().mapToLong(LogFileInfo::getSize).sum();
        long maxSize = parseSize(totalSizeCap);

        log.info("当前日志文件数量: {}, 总大小: {}, 限制: {}", logFiles.size(), formatSize(totalSize), totalSizeCap);

        if (totalSize <= maxSize) {
            log.info("日志文件总大小未超过限制，无需清理");
            return;
        }

        // 按最后修改时间升序排序（最旧的在前）
        logFiles.sort(Comparator.comparingLong(LogFileInfo::getLastModified));

        long targetSize = (long) (maxSize * 0.9); // 清理到限制的90%
        int deletedCount = 0;
        long deletedSize = 0;

        for (LogFileInfo info : logFiles) {
            if (totalSize - deletedSize <= targetSize) {
                break;
            }
            Files.delete(info.getPath());
            deletedSize += info.getSize();
            deletedCount++;
            log.info("删除旧日志文件: {}", info.getPath().getFileName());
        }

        log.info("日志清理完成，删除文件数: {}, 释放空间: {}", deletedCount, formatSize(deletedSize));
    }

    private List<LogFileInfo> findLogFiles(Path logDir) throws IOException {
        try (Stream<Path> stream = Files.list(logDir)) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(p -> LOG_FILE_PATTERN.matcher(p.getFileName().toString()).matches())
                    .map(this::toLogFileInfo)
                    .collect(Collectors.toList());
        }
    }

    private LogFileInfo toLogFileInfo(Path path) {
        try {
            long size = Files.size(path);
            long lastModified = Files.getLastModifiedTime(path).toMillis();
            return new LogFileInfo(path, size, lastModified);
        } catch (IOException e) {
            log.warn("无法读取文件信息: {}", path, e);
            return new LogFileInfo(path, 0, 0);
        }
    }

    private long parseSize(String sizeStr) {
        sizeStr = sizeStr.trim().toUpperCase();
        long multiplier = 1;
        
        if (sizeStr.endsWith("GB")) {
            multiplier = 1024L * 1024L * 1024L;
            sizeStr = sizeStr.substring(0, sizeStr.length() - 2).trim();
        } else if (sizeStr.endsWith("MB")) {
            multiplier = 1024L * 1024L;
            sizeStr = sizeStr.substring(0, sizeStr.length() - 2).trim();
        } else if (sizeStr.endsWith("KB")) {
            multiplier = 1024L;
            sizeStr = sizeStr.substring(0, sizeStr.length() - 2).trim();
        }
        
        return Long.parseLong(sizeStr) * multiplier;
    }

    private String formatSize(long bytes) {
        if (bytes >= 1024L * 1024L * 1024L) {
            return String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0));
        } else if (bytes >= 1024L * 1024L) {
            return String.format("%.2f MB", bytes / (1024.0 * 1024.0));
        } else if (bytes >= 1024L) {
            return String.format("%.2f KB", bytes / 1024.0);
        } else {
            return bytes + " B";
        }
    }

    private static class LogFileInfo {
        private final Path path;
        private final long size;
        private final long lastModified;

        LogFileInfo(Path path, long size, long lastModified) {
            this.path = path;
            this.size = size;
            this.lastModified = lastModified;
        }

        Path getPath() {
            return path;
        }

        long getSize() {
            return size;
        }

        long getLastModified() {
            return lastModified;
        }
    }
}
```

---

### Task 4: 编译验证

**Step 1: 编译项目**

Run: `./mvnw clean compile`
Expected: BUILD SUCCESS

**Step 2: 运行项目验证**

Run: `./mvnw spring-boot:run`
Expected: 
- 应用正常启动
- 控制台输出日志清理相关信息
- `./logs/` 目录下生成带时间戳的日志文件

---

### Task 5: 提交代码

**Step 1: 提交变更**

```bash
git add src/main/resources/logback-spring.xml src/main/resources/application.properties src/main/java/top/enderliquid/audioflow/common/task/LogCleanupRunner.java
git commit -m "添加日志文件输出功能：每次启动生成独立日志文件，支持总大小限制"
```