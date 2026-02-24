package top.enderliquid.audioflow.manager.impl;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.lang.Nullable;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.time.Duration;

@Slf4j
@Component
public class OSSManagerImpl implements top.enderliquid.audioflow.manager.OSSManager {

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
            log.warn("S3 凭证未配置，OSSManagerImpl 将不会被初始化");
            return;
        }
        AwsBasicCredentials credentials = AwsBasicCredentials.create(accessKey, secretKey);
        StaticCredentialsProvider credentialsProvider = StaticCredentialsProvider.create(credentials);
        S3Configuration s3Config = S3Configuration.builder()
                .pathStyleAccessEnabled(false)
                .build();

        String resolvedEndpoint = resolveEndpointTemplate(endpoint, region, bucketName);

        software.amazon.awssdk.services.s3.S3ClientBuilder clientBuilder = S3Client.builder()
                .credentialsProvider(credentialsProvider)
                .serviceConfiguration(s3Config)
                .region(Region.of(region));
        if (resolvedEndpoint != null && !resolvedEndpoint.isEmpty()) {
            clientBuilder.endpointOverride(URI.create(resolvedEndpoint));
        }
        s3Client = clientBuilder.build();
        software.amazon.awssdk.services.s3.presigner.S3Presigner.Builder presignerBuilder = S3Presigner.builder()
                .credentialsProvider(credentialsProvider)
                .region(Region.of(region));
        if (resolvedEndpoint != null && !resolvedEndpoint.isEmpty()) {
            presignerBuilder.endpointOverride(URI.create(resolvedEndpoint));
        }
        s3Presigner = presignerBuilder.build();
        log.info("OSSManagerImpl 初始化完成，存储桶名称: {}, endpoint: {}", bucketName,
                resolvedEndpoint != null ? resolvedEndpoint : "AWS 默认");
    }

    private String resolveEndpointTemplate(String endpoint, String region, String bucket) {
        if (endpoint == null || endpoint.isEmpty()) {
            return null;
        }
        String result = endpoint;
        if (result.contains("{region}")) {
            result = result.replace("{region}", region);
        }
        if (result.contains("{bucket}")) {
            result = result.replace("{bucket}", bucket);
        }
        return result;
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
    @Nullable
    public String generatePresignedPostUrl(String fileName, Duration expiration) {
        if (s3Presigner == null) {
            log.error("S3Presigner 未初始化");
            return null;
        }
        try {
            PresignedPutObjectRequest presignedRequest = s3Presigner.presignPutObject(
                    software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest.builder()
                            .signatureDuration(expiration)
                            .putObjectRequest(builder -> builder
                                    .bucket(bucketName)
                                    .key(fileName))
                            .build()
            );
            return presignedRequest.url().toString();
        } catch (SdkException e) {
            log.error("生成预签名上传URL失败，文件名: {}", fileName, e);
            return null;
        }
    }

    @Override
    public boolean checkFileExists(String fileName) {
        if (s3Client == null) {
            log.error("S3Client 未初始化");
            return false;
        }
        try {
            HeadObjectRequest headRequest = HeadObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fileName)
                    .build();
            s3Client.headObject(headRequest);
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
        if (s3Client == null) {
            log.error("S3Client 未初始化");
            return null;
        }
        try {
            GetObjectRequest getRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fileName)
                    .build();
            return s3Client.getObject(getRequest);
        } catch (SdkException e) {
            log.error("获取文件流失败，文件名: {}", fileName, e);
            return null;
        }
    }

    @Override
    public boolean deleteFile(String fileName) {
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

    @Override
    @Nullable
    public String getPresignedGetUrl(String fileName, Duration expiration) {
        if (s3Presigner == null) {
            log.error("S3Presigner 未初始化");
            return null;
        }
        try {
            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(expiration)
                    .getObjectRequest(builder -> builder
                            .bucket(bucketName)
                            .key(fileName))
                    .build();
            PresignedGetObjectRequest presignedRequest = s3Presigner.presignGetObject(presignRequest);
            return presignedRequest.url().toString();
        } catch (SdkException e) {
            log.error("生成预签名访问URL失败，文件名: {}", fileName, e);
            return null;
        }
    }

    @Override
    @Nullable
    public Long getFileSize(String fileName) {
        if (s3Client == null) {
            log.error("S3Client 未初始化");
            return null;
        }
        try {
            HeadObjectRequest headRequest = HeadObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fileName)
                    .build();
            HeadObjectResponse response = s3Client.headObject(headRequest);
            return response.contentLength();
        } catch (SdkException e) {
            log.error("获取文件大小失败，文件名: {}", fileName, e);
            return null;
        }
    }
}