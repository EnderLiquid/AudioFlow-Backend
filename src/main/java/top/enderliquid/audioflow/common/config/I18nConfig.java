package top.enderliquid.audioflow.common.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.LocaleResolver;

import java.util.Locale;

@Configuration
public class I18nConfig {

    @Bean
    public LocaleResolver localeResolver() {
        return new LocaleResolver() {
            @Override
            @NonNull
            public Locale resolveLocale(@NonNull HttpServletRequest request) {
                String language = request.getHeader("Accept-Language"); // 例如: zh-CN
                String style = request.getHeader("X-App-Style");        // 例如: meow

                Locale.Builder builder = new Locale.Builder();

                if (language != null && !language.isBlank()) {
                    String[] split = language.split("-");
                    builder.setLanguage(split[0]);
                    if (split.length > 1) builder.setRegion(split[1]);
                } else {
                    builder.setLanguage("zh").setRegion("CN"); // 默认中文
                }

                // 注入变体风格
                if (style != null && !style.isBlank()) {
                    builder.setVariant(style);
                }
                return builder.build();
            }

            @Override
            public void setLocale(@NonNull HttpServletRequest request, HttpServletResponse response, Locale locale) {}
        };
    }
}