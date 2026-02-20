package top.enderliquid.audioflow.manager.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import top.enderliquid.audioflow.manager.FileManager;
import top.enderliquid.audioflow.manager.strategy.file.FileStorageStrategy;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Repository
public class FileManagerImpl implements FileManager {

    @Value("${file.storage.active}")
    private String activeStorageType;

    public FileManagerImpl(List<FileStorageStrategy> strategies) {
        this.strategyMap = strategies.stream()
                .collect(Collectors.toMap(FileStorageStrategy::getType, Function.identity()));
    }

    private final Map<String, FileStorageStrategy> strategyMap;

    @Override
    public String save(String fileName, InputStream content, String mimeType) {
        FileStorageStrategy strategy = strategyMap.get(activeStorageType);
        if (strategy == null) {
            log.error("文件保存策略不存在");
            return null;
        }
        if (!strategy.save(fileName, content, mimeType)) return null;
        return activeStorageType;
    }

    @Override
    public String getUrl(String fileName, String sourceType) {
        FileStorageStrategy strategy = strategyMap.get(sourceType);
        if (strategy == null) {
            log.error("文件保存策略不存在");
            return null;
        }
        return strategy.getUrl(fileName);
    }

    @Override
    public boolean delete(String fileName, String sourceType) {
        FileStorageStrategy strategy = strategyMap.get(sourceType);
        if (strategy == null) {
            log.error("文件保存策略不存在");
            return false;
        }
        return strategy.delete(fileName);
    }
}
