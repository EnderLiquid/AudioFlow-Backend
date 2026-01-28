package top.enderliquid.audioflow.manager;

import org.springframework.web.multipart.MultipartFile;

public interface FileManager {
    boolean save(MultipartFile file, String fileName);

    boolean delete(String fileName);
}
