package top.enderliquid.audioflow.manager;

import org.springframework.web.multipart.MultipartFile;
import top.enderliquid.audioflow.common.constant.FileConstant;

import java.io.IOException;
import java.nio.file.Path;

public interface FileManager {
    public Path saveMultipartFileToDisk(MultipartFile file, Path dirPath, String fileName) throws IOException;

    public FileConstant.DeleteResult deleteFileFromDisk(Path path);
}
