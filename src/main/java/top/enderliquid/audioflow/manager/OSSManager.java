package top.enderliquid.audioflow.manager;

import org.springframework.lang.Nullable;

import java.io.InputStream;
import java.time.Duration;

public interface OSSManager {
    /**
     * 生成预签名POST上传URL（带Policy限制）
     *
     * @param fileName   文件名
     * @return 预签名URL
     */
    @Nullable
    String generatePresignedPutUrl(String fileName, String mimeType);

    /**
     * 检查文件是否存在
     *
     * @param fileName 文件名
     * @return 是否存在
     */
    boolean checkFileExists(String fileName);

    /**
     * 获取文件InputStream
     *
     * @param fileName 文件名
     * @return 文件流
     */
    @Nullable
    InputStream getFileInputStream(String fileName);

    /**
     * 删除文件
     *
     * @param fileName 文件名
     * @return 删除是否成功
     */
    boolean deleteFile(String fileName);

    /**
     * 生成预签名上传 URL (PUT 方法)
     * 前端需使用 PUT 请求，并带上对应的 Content-Type
     *
     * @param fileName   文件名
     * @param expiration 过期时间
     * @return 预签名URL
     */
    @Nullable
    String getPresignedGetUrl(String fileName, Duration expiration);

    /**
     * 获取文件大小
     *
     * @param fileName 文件名
     * @return 文件大小（字节）
     */
    @Nullable
    Long getFileSize(String fileName);
}