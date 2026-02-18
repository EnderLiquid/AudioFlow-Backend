package top.enderliquid.audioflow.manager.strategy.file;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.io.InputStream;
import java.net.URI;
import java.time.Duration;

@Slf4j
@Component
public class S3StorageStrategy implements FileStorageStrategy {

    @Value("${file.storage.s3.endpoint}")
    private String endpoint;

    @Value("${file.storage.s3.region}")
    private String region;

    @Value("${file.storage.s3.access-key}")
    private String accessKey;

    @Value("${file.storage.s3.secret-key}")
    private String secretKey;

    @Value("${file.storage.s3.bucket-name}")
    private String bucketName;

    @Value("${file.storage.s3.presigned-url-expiration:3600}")
    private int presignedUrlExpirationSeconds;

    private S3Client s3Client;
    private S3Presigner s3Presigner;

    @PostConstruct
    public void init() {
        if (accessKey == null || accessKey.isEmpty() || secretKey == null || secretKey.isEmpty()) {
            log.warn("S3 凭证未配置，S3StorageStrategy 将不会被初始化");
            return;
        }
        AwsBasicCredentials credentials = AwsBasicCredentials.create(accessKey, secretKey);
        StaticCredentialsProvider credentialsProvider = StaticCredentialsProvider.create(credentials);
        S3Configuration s3Config = S3Configuration.builder()
                .pathStyleAccessEnabled(true)
                .build();
        software.amazon.awssdk.services.s3.S3ClientBuilder clientBuilder = S3Client.builder()
                .credentialsProvider(credentialsProvider)
                .serviceConfiguration(s3Config)
                .region(Region.of(region));
        if (endpoint != null && !endpoint.isEmpty()) {
            clientBuilder.endpointOverride(URI.create(endpoint));
        }
        s3Client = clientBuilder.build();
        software.amazon.awssdk.services.s3.presigner.S3Presigner.Builder presignerBuilder = S3Presigner.builder()
                .credentialsProvider(credentialsProvider)
                .region(Region.of(region));
        if (endpoint != null && !endpoint.isEmpty()) {
            presignerBuilder.endpointOverride(URI.create(endpoint));
        }
        s3Presigner = presignerBuilder.build();
        log.info("S3StorageStrategy 初始化完成，存储桶名称: {}", bucketName);
    }

    @PreDestroy
    public void destroy() {
        if (s3Client != null) {
            s3Client.close();
        }
        if (s3Presigner != null) {
            s3Presigner.close();
        }
    }

    @Override
    public String getType() {
        return "s3";
    }

    @Override
    public boolean save(String fileName, InputStream content) {
        if (s3Client == null) {
            log.error("S3Client 未初始化");
            return false;
        }
        try {
            PutObjectRequest putRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fileName)
                    .build();
            s3Client.putObject(putRequest, RequestBody.fromInputStream(content, content.available()));
            log.debug("文件上传成功，文件名: {}，存储桶名称: {}", fileName, bucketName);
            return true;
        } catch (SdkException e) {
            log.error("文件上传失败，文件名: {}", fileName, e);
            return false;
        } catch (Exception e) {
            log.error("文件上传发生未知错误，文件名: {}", fileName, e);
            return false;
        }
    }

    @Override
    public String getUrl(String fileName) {
        if (s3Presigner == null) {
            log.error("S3Presigner 未初始化");
            return null;
        }
        try {
            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofSeconds(presignedUrlExpirationSeconds))
                    .getObjectRequest(builder -> builder
                            .bucket(bucketName)
                            .key(fileName))
                    .build();
            PresignedGetObjectRequest presignedRequest = s3Presigner.presignGetObject(presignRequest);
            return presignedRequest.url().toString();
        } catch (SdkException e) {
            log.error("生成预签名 URL 失败，文件名: {}", fileName, e);
            return null;
        }
    }

    @Override
    public boolean delete(String fileName) {
        if (s3Client == null) {
            log.error("S3Client 未初始化");
            return false;
        }
        try {
            DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fileName)
                    .build();
            s3Client.deleteObject(deleteRequest);
            log.debug("文件删除成功，文件名: {}，存储桶名称: {}", fileName, bucketName);
            return true;
        } catch (SdkException e) {
            log.error("文件删除失败，文件名: {}", fileName, e);
            return false;
        }
    }
}
