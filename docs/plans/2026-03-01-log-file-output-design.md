# 日志文件输出功能设计

## 需求概述

实现日志输出到文件的功能，满足以下要求：
1. 每次启动项目生成独立日志文件
2. 限制日志文件总大小
3. 默认限制 100MB，可通过配置调整

## 技术方案

### 方案选择

使用 logback 的 `<timestamp>` 元素生成启动时间戳，配合 Spring Boot 启动时执行的清理组件实现需求。

### 核心组件

1. **logback-spring.xml** - 配置文件输出 appender
2. **LogCleanupRunner** - 日志清理组件，启动时执行
3. **application.properties** - 配置项

## 详细设计

### 1. logback-spring.xml 配置

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

### 2. 日志清理组件

**类名**: `LogCleanupRunner`  
**包路径**: `com.audioflow.config`  
**功能**:
- 实现 `ApplicationRunner` 接口，在应用启动时执行
- 扫描 `./logs/` 目录下的日志文件
- 按修改时间排序，计算总大小
- 超过限制时删除最旧的文件

**配置属性**:
```properties
# 日志文件总大小限制（默认100MB，支持KB/MB/GB后缀）
logging.total-size-cap=100MB
```

### 3. 日志文件命名规则

格式: `AudioFlow-{启动时间}.log`  
示例: `AudioFlow-2026-03-01_14-30-25.log`

### 4. 清理逻辑

1. 获取日志目录下所有匹配 `AudioFlow-*.log` 的文件
2. 按最后修改时间降序排序
3. 计算总大小，若超过限制则从最旧的文件开始删除
4. 删除直到总大小低于限制的 90%（留缓冲空间）

## 文件变更清单

| 文件 | 操作 | 说明 |
|-----|------|------|
| `src/main/resources/logback-spring.xml` | 修改 | 添加文件输出 appender |
| `src/main/java/com/audioflow/config/LogCleanupRunner.java` | 新增 | 日志清理组件 |
| `src/main/resources/application.properties` | 修改 | 添加配置项 |

## 实现步骤

1. 修改 logback-spring.xml，添加文件输出配置
2. 创建 LogCleanupRunner 类
3. 添加配置项到 application.properties
4. 测试验证

## 注意事项

- 日志目录 `./logs/` 会自动创建
- 清理操作在应用启动完成后执行，不影响启动性能
- 清理日志使用 INFO 级别，便于观察