package top.enderliquid.audioflow.manager;

import org.springframework.lang.Nullable;

import java.io.InputStream;
import java.time.Duration;

public interface OSSManager {
    /**
     * 生成预签名POST上传URL（带Policy限制）
     *
     * @param fileName 文件名
     * @param expiration 过期时间
     * @return 预签名URL
     */
    @Nullable
    String generatePresignedPostUrl(String fileName, Duration expiration);

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
     * 获取预签名访问URL（播放用）
     *
     * @param fileName 文件名
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