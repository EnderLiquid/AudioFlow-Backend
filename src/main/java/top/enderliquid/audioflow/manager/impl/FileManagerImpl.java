package top.enderliquid.audioflow.manager.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import org.springframework.web.multipart.MultipartFile;
import top.enderliquid.audioflow.manager.FileManager;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

@Slf4j
@Repository
public class FileManagerImpl implements FileManager {

    @Value("${file.storage.local.dir}")
    private String localStorageDir;

    public boolean save(MultipartFile file, String fileName) {
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
        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, tempFilePath, StandardCopyOption.REPLACE_EXISTING);
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
