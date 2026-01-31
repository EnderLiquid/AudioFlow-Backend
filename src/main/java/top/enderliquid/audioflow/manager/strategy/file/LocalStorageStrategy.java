package top.enderliquid.audioflow.manager.strategy.file;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

@Slf4j
@Component
public class LocalStorageStrategy implements FileStorageStrategy {

    @Value("${file.storage.local.storage-dir}")
    private String localStorageDir;

    @Value("${file.storage.local.url-prefix}")
    private String localUrlPrefix;

    @Override
    public String getType() {
        return "local";
    }

    public boolean save(String fileName, InputStream content) {
        // 创建目录
        Path dirPath;
        try {
            dirPath = Path.of(localStorageDir);
        } catch (InvalidPathException e) {
            log.error("解析路径失败", e);
            return false;
        }
        try {
            if (!Files.isDirectory(dirPath)) {
                Files.createDirectories(dirPath);
            }
        } catch (IOException e) {
            log.error("创建文件保存目录失败", e);
            return false;
        }
        String tempFileName = fileName + ".tmp";
        Path filePath, tempFilePath;
        try {
            filePath = dirPath.resolve(fileName);
            tempFilePath = dirPath.resolve(tempFileName);
        } catch (InvalidPathException e) {
            log.error("解析路径失败", e);
            return false;
        }
        // 创建临时文件
        try (content) {
            Files.copy(content, tempFilePath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            // 执行回滚
            try {
                // 创建临时文件失败，临时文件可能存在也可能不存在
                Files.deleteIfExists(tempFilePath);
            } catch (IOException ex) {
                log.error("写入临时文件 {} 失败，且删除临时文件也失败", tempFilePath, ex);
            }
            return false;
        }
        // 重命名临时文件（原子操作）
        try {
            Files.move(tempFilePath, filePath, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            // 执行回滚
            try {
                // 重命名失败，临时文件仍然存在
                Files.delete(tempFilePath);
            } catch (IOException ex) {
                log.error("重命名临时文件 {} 到目标文件 {} 失败，且删除临时文件也失败", tempFilePath, filePath, ex);
            }
            return false;
        }
        return true;
    }

    @Override
    public String getUrl(String fileName) {
        return localUrlPrefix + fileName;
    }

    @Override
    public boolean delete(String fileName) {
        Path filePath;
        try {
            filePath = Path.of(localStorageDir).resolve(fileName);
        } catch (InvalidPathException e) {
            log.error("解析路径失败");
            return false;
        }
        try {
            if (!Files.exists(filePath) || !Files.isRegularFile(filePath)) {
                return false;
            }
            Files.delete(filePath);
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}
