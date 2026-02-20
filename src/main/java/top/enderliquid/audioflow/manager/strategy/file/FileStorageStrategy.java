package top.enderliquid.audioflow.manager.strategy.file;

import java.io.InputStream;

public interface FileStorageStrategy {
    /**
     * 获取策略类型，如 "local", "alioss"
     */
    String getType();

    /**
     * 上传文件
     *
     * @param fileName 文件名 (如song.mp3)
     * @param content  文件流
     * @param mimeType 文件 MIME 类型，用于设置 Content-Type
     * @return 保存是否成功
     */
    boolean save(String fileName, InputStream content, String mimeType);

    /**
     * 获取可访问的完整 URL
     *
     * @param fileName 文件名
     */
    String getUrl(String fileName);

    /**
     * 删除文件
     *
     * @return 删除是否成功
     */
    boolean delete(String fileName);
}

