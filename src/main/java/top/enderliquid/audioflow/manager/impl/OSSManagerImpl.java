package top.enderliquid.audioflow.manager.impl;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;
import top.enderliquid.audioflow.manager.OSSManager;

import java.io.InputStream;
import java.net.URI;
import java.time.Duration;

@Slf4j
@Component
public class OSSManagerImpl implements OSSManager {

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

    @Value("${file.storage.s3.presigned-url-expiration}")
    private int presignedUrlExpirationSeconds;

    @Value("${file.storage.s3.path-style-access}")
    private boolean pathStyleAccess;

    private S3Client s3Client;
    private S3Presigner s3Presigner;

    @PostConstruct
    public void init() {
        if (accessKey == null || accessKey.isEmpty() || secretKey == null || secretKey.isEmpty()) {
            log.warn("S3 凭证未配置，OSSManagerImpl 将不会被初始化");
            return;
        }

        AwsBasicCredentials credentials = AwsBasicCredentials.create(accessKey, secretKey);
        StaticCredentialsProvider credentialsProvider = StaticCredentialsProvider.create(credentials);

        // 配置 S3 客户端
        S3Configuration s3Config = S3Configuration.builder()
                .pathStyleAccessEnabled(pathStyleAccess) // 使用配置项
                .chunkedEncodingEnabled(false) // 某些兼容 S3 可能需要关闭 chunked
                .build();

        // 构建 S3Client
        software.amazon.awssdk.services.s3.S3ClientBuilder clientBuilder = S3Client.builder()
                .credentialsProvider(credentialsProvider)
                .serviceConfiguration(s3Config)
                .region(Region.of(region));

        // 构建 Presigner
        software.amazon.awssdk.services.s3.presigner.S3Presigner.Builder presignerBuilder = S3Presigner.builder()
                .credentialsProvider(credentialsProvider)
                .serviceConfiguration(s3Config)
                .region(Region.of(region));

        if (endpoint != null && !endpoint.isEmpty()) {
            URI endpointUri = URI.create(endpoint);
            clientBuilder.endpointOverride(endpointUri);
            presignerBuilder.endpointOverride(endpointUri);
        }

        s3Client = clientBuilder.build();
        s3Presigner = presignerBuilder.build();

        log.info("OSSManagerImpl 初始化完成. Bucket: {}, Region: {}, PathStyle: {}",
                bucketName, region, pathStyleAccess);
    }

    @PreDestroy
    public void destroy() {
        if (s3Client != null) s3Client.close();
        if (s3Presigner != null) s3Presigner.close();
    }

    @Override
    @Nullable
    public String generatePresignedPutUrl(String fileName, String mimeType) {
        if (s3Presigner == null) return null;
        try {
            Duration expiration = Duration.ofSeconds(presignedUrlExpirationSeconds);
            PresignedPutObjectRequest presignedRequest = s3Presigner.presignPutObject(
                    PutObjectPresignRequest.builder()
                            .signatureDuration(expiration)
                            .putObjectRequest(builder -> builder
                                    .bucket(bucketName)
                                    .key(fileName)
                                    .contentType(mimeType)
                                    .contentDisposition("inline")
                            )
                            .build()
            );

            // 前端上传时 Header 必须包含:
            // Content-Type: [mimeType]
            // Content-Disposition: inline
            return presignedRequest.url().toString();
        } catch (SdkException e) {
            log.error("生成预签名上传URL失败，文件名: {}", fileName, e);
            return null;
        }
    }

    @Override
    public boolean checkFileExists(String fileName) {
        if (s3Client == null) return false;
        try {
            s3Client.headObject(builder -> builder.bucket(bucketName).key(fileName));
            return true;
        } catch (NoSuchKeyException e) {
            return false;
        } catch (SdkException e) {
            log.error("检查文件是否存在失败，文件名: {}", fileName, e);
            return false;
        }
    }

    @Override
    @Nullable
    public InputStream getFileInputStream(String fileName) {
        if (s3Client == null) return null;
        try {
            return s3Client.getObject(builder -> builder.bucket(bucketName).key(fileName));
        } catch (SdkException e) {
            log.error("获取文件流失败，文件名: {}", fileName, e);
            return null;
        }
    }

    @Override
    public boolean deleteFile(String fileName) {
        if (s3Client == null) return false;
        try {
            s3Client.deleteObject(builder -> builder.bucket(bucketName).key(fileName));
            log.debug("文件删除请求已发送，文件名: {}", fileName);
            return true;
        } catch (SdkException e) {
            log.error("文件删除失败，文件名: {}", fileName, e);
            return false;
        }
    }

    @Override
    @Nullable
    public String getPresignedGetUrl(String fileName, Duration expiration) {
        if (s3Presigner == null) return null;
        try {
            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(expiration)
                    .getObjectRequest(builder -> builder.bucket(bucketName).key(fileName))
                    .build();
            return s3Presigner.presignGetObject(presignRequest).url().toString();
        } catch (SdkException e) {
            log.error("生成下载URL失败，文件名: {}", fileName, e);
            return null;
        }
    }

    @Override
    @Nullable
    public Long getFileSize(String fileName) {
        if (s3Client == null) return null;
        try {
            HeadObjectResponse response = s3Client.headObject(builder -> builder.bucket(bucketName).key(fileName));
            return response.contentLength();
        } catch (SdkException e) {
            log.error("获取文件大小失败，文件名: {}", fileName, e);
            return null;
        }
    }
}
