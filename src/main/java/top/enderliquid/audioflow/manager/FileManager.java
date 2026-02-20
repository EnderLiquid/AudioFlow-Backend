package top.enderliquid.audioflow.manager;

import org.springframework.lang.Nullable;

import java.io.InputStream;

public interface FileManager {

    /**
     * 上传文件
     *
     * @param fileName 文件名
     * @param content 文件流
     * @param mimeType 文件 MIME 类型
     * @return 返回存储源类型 (sourceType)，若保存失败则返回 null
     */
    @Nullable
    String save(String fileName, InputStream content, String mimeType);

    /**
     * 获取文件访问 URL
     *
     * @return 文件访问 URL，若获取失败则返回 null
     */
    @Nullable
    String getUrl(String fileName, String sourceType);

    /**
     * 删除文件
     *
     * @return 删除是否成功
     */
    boolean delete(String fileName, String sourceType);
}
