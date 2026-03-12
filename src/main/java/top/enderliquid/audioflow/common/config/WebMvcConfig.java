package top.enderliquid.audioflow.common.config;

import cn.dev33.satoken.interceptor.SaInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import top.enderliquid.audioflow.common.filter.HttpMethodOverrideFilter;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Value("${app.cors.allowed-origin-patterns}")
    private String[] allowedOriginPatterns;

    @Autowired
    private RateLimitInterceptor rateLimitInterceptor;

    @Bean
    public FilterRegistrationBean<HttpMethodOverrideFilter> httpMethodOverrideFilterRegistration() {
        FilterRegistrationBean<HttpMethodOverrideFilter> registration = new FilterRegistrationBean<>(new HttpMethodOverrideFilter());
        // 确保过滤器在大多数其他过滤器之前运行，并在Spring MVC分发之前执行
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 10);
        return registration;
    }

    @Override
    public void addCorsMappings(@NonNull CorsRegistry registry) {
        // CORS配置
        if (allowedOriginPatterns != null && allowedOriginPatterns.length > 0) {
            registry.addMapping("/**")
                    .allowedOriginPatterns(allowedOriginPatterns)
                    .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                    .allowedHeaders("*")
                    .allowCredentials(true)
                    .maxAge(3600);
        }
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 注册限流拦截器
        registry.addInterceptor(rateLimitInterceptor).addPathPatterns("/**");
        // 注册 Sa-Token 鉴权拦截器
        registry.addInterceptor(new SaInterceptor()).addPathPatterns("/**");
    }
}


