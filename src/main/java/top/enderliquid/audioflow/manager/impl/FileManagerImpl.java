package top.enderliquid.audioflow.manager.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;
import top.enderliquid.audioflow.common.constant.FileConstant;
import top.enderliquid.audioflow.manager.FileManager;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Slf4j
public class FileManagerImpl implements FileManager {
    public Path saveMultipartFileToDisk(MultipartFile file, Path dirPath, String fileName) throws IOException {
        //创建目录
        if (!Files.isDirectory(dirPath)) {
            try {
                Files.createDirectories(dirPath);
            } catch (IOException e) {
                throw new IOException("创建目录 {%s} 失败".formatted(dirPath), e);
            }
        }
        Path filePath = dirPath.resolve(fileName);
        String tempFileName = fileName + ".tmp_" + UUID.randomUUID();
        Path tempFilePath = dirPath.resolve(tempFileName);
        //创建临时文件
        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, tempFilePath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            try {
                Files.deleteIfExists(tempFilePath);
            } catch (IOException ex) {
                FileConstant.DeleteResult deleteResult = deleteFileFromDisk(tempFilePath);
                if (deleteResult == FileConstant.DeleteResult.IO_ERROR) {
                    log.error("写入临时文件 {} 失败，且删除临时文件也失败", tempFilePath);
                }
            }
            throw new IOException("写入临时文件 {%s} 失败".formatted(tempFilePath), e);
        }
        //重命名临时文件（原子操作）
        try {
            Files.move(tempFilePath, filePath, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            try {
                Files.deleteIfExists(tempFilePath);
            } catch (IOException ex) {
                FileConstant.DeleteResult deleteResult = deleteFileFromDisk(tempFilePath);
                if (deleteResult == FileConstant.DeleteResult.IO_ERROR) {
                    log.error("重命名临时文件 {} 到目标文件 {} 失败，且删除临时文件也失败", tempFilePath, filePath);
                }
            }
            throw new IOException("重命名临时文件 {%s} 到目标文件 {%s} 失败".formatted(tempFilePath, filePath), e);
        }
        return filePath;
    }

    @Override
    public FileConstant.DeleteResult deleteFileFromDisk(Path path) {
        try {
            if (!Files.exists(path)) {
                return FileConstant.DeleteResult.NOT_EXISTS;
            }
            if (!Files.isRegularFile(path)) {
                return FileConstant.DeleteResult.NOT_REGULAR_FILE;
            }
            Files.delete(path);
            return FileConstant.DeleteResult.SUCCESS;
        } catch (IOException e) {
            return FileConstant.DeleteResult.IO_ERROR;
        }
    }
}
