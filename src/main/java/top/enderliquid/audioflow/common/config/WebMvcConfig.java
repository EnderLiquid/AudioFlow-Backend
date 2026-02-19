package top.enderliquid.audioflow.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
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

    @Bean
    public FilterRegistrationBean<HttpMethodOverrideFilter> httpMethodOverrideFilterRegistration() {
        FilterRegistrationBean<HttpMethodOverrideFilter> registration = new FilterRegistrationBean<>(new HttpMethodOverrideFilter());
        // 确保过滤器在大多数其他过滤器之前运行，并在Spring MVC分发之前执行
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 10);
        return registration;
    }
}


