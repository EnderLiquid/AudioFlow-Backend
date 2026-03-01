package top.enderliquid.audioflow.common.task;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.FileAppender;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
        String currentLogFilePath = getCurrentLogFilePath();
        
        log.info("开始检查日志文件大小限制");
        try {
            cleanupOldLogs(currentLogFilePath);
        } catch (Exception e) {
            log.warn("清理旧日志文件失败", e);
        }
    }

    private String getCurrentLogFilePath() {
        try {
            LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
            Logger rootLogger = context.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
            Appender<ch.qos.logback.classic.spi.ILoggingEvent> appender = rootLogger.getAppender("FILE");
            
            if (appender instanceof FileAppender) {
                String fileName = ((FileAppender<?>) appender).getFile();
                if (fileName != null) {
                    return new File(fileName).getCanonicalPath();
                }
            }
        } catch (Exception e) {
            log.warn("获取当前日志文件路径失败", e);
        }
        return null;
    }

    private void cleanupOldLogs(String currentLogFilePath) throws IOException {
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

        // 排除当前正在使用的日志文件
        List<LogFileInfo> filesToConsider;
        if (currentLogFilePath != null) {
            filesToConsider = logFiles.stream()
                    .filter(f -> {
                        try {
                            return !f.getPath().toFile().getCanonicalPath().equals(currentLogFilePath);
                        } catch (IOException e) {
                            return true;
                        }
                    })
                    .collect(Collectors.toList());
        } else {
            // 如果没法确定当前文件名，保守起见排除最后修改时间最新的一个文件
            logFiles.sort(Comparator.comparingLong(LogFileInfo::getLastModified).reversed());
            filesToConsider = logFiles.stream().skip(1).collect(Collectors.toList());
        }

        if (filesToConsider.isEmpty()) {
            log.info("除当前日志外没有其他日志文件，跳过清理");
            return;
        }

        long totalSize = logFiles.stream().mapToLong(LogFileInfo::getSize).sum();
        long maxSize = parseSize(totalSizeCap);

        log.info("当前日志文件数量: {}, 总大小: {}, 限制: {}", logFiles.size(), formatSize(totalSize), totalSizeCap);

        if (totalSize <= maxSize) {
            log.info("日志文件总大小未超过限制，无需清理");
            return;
        }

        filesToConsider.sort(Comparator.comparingLong(LogFileInfo::getLastModified));

        long targetSize = (long) (maxSize * 0.9);
        int deletedCount = 0;
        long deletedSize = 0;

        for (LogFileInfo info : filesToConsider) {
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