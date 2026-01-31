package top.enderliquid.audioflow.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
// 配置静态资源映射
public class WebMvcConfig implements WebMvcConfigurer {
    @Value("${file.storage.local.storage-dir}")
    private String localStorageDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/file/**") // 网页url后缀
                .addResourceLocations("file:" + localStorageDir); // 磁盘路径
    }
}

