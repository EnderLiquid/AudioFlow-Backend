package top.enderliquid.audioflow.common;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import top.enderliquid.audioflow.manager.OSSManager;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@TestConfiguration
public class MockOSSConfig {

    @Bean
    @Primary
    public OSSManager mockOSSManager() {
        return new MockOSSManager();
    }

    public static class MockOSSManager implements OSSManager {
        private final Map<String, byte[]> fileStorage = new HashMap<>();

        @Override
        public String generatePresignedPutUrl(String fileName, String mimeType) {
            return "https://mock-test-s3.example.com/upload/" + fileName;
        }

        @Override
        public boolean checkFileExists(String fileName) {
            return fileStorage.containsKey(fileName);
        }

        @Override
        public InputStream getFileInputStream(String fileName) {
            byte[] content = fileStorage.get(fileName);
            if (content == null) {
                return null;
            }
            return new ByteArrayInputStream(content);
        }

        @Override
        public boolean deleteFile(String fileName) {
            return fileStorage.remove(fileName) != null;
        }

        @Override
        public String getPresignedGetUrl(String fileName, Duration expiration) {
            return "https://mock-test-s3.example.com/download/" + fileName;
        }

        @Override
        public Long getFileSize(String fileName) {
            byte[] content = fileStorage.get(fileName);
            if (content == null) {
                return null;
            }
            return (long) content.length;
        }

        public void simulateUpload(String fileName) throws IOException {
            Path testAudioFile = Paths.get("src/test/resources/audio/test-song.mp3");
            byte[] audioData = Files.readAllBytes(testAudioFile);
            fileStorage.put(fileName, audioData);
        }

        public void clearAll() {
            fileStorage.clear();
        }
    }
}
