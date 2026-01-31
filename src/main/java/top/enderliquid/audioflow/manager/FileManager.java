package top.enderliquid.audioflow.manager;

import org.springframework.web.multipart.MultipartFile;

public interface FileManager {
    boolean save(String fileName, MultipartFile file);

    String getUrl(String fileName, String sourceType);

    boolean delete(String fileName);
}
