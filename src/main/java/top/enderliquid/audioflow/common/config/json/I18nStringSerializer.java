package top.enderliquid.audioflow.common.config.json;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.context.i18n.LocaleContextHolder;

import java.io.IOException;
import java.util.Arrays;

public class I18nStringSerializer extends JsonSerializer<String> {

    private final MessageSource messageSource;
    private static final String PREFIX = "#I18N{";
    private static final String SUFFIX = "}";
    private static final String SEPARATOR = "\\|";

    // 构造器注入 Spring 的 MessageSource
    public I18nStringSerializer(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    @Override
    public void serialize(String value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        // 1. 拦截 DSL 格式
        if (value != null && value.startsWith(PREFIX) && value.endsWith(SUFFIX)) {
            // 剥离前后缀
            String content = value.substring(PREFIX.length(), value.length() - SUFFIX.length());
            String[] parts = content.split(SEPARATOR);

            String key = parts[0];
            // 获取动态参数
            Object[] args = parts.length > 1 ? Arrays.copyOfRange(parts, 1, parts.length) : null;

            try {
                // 执行翻译
                String translated = messageSource.getMessage(key, args, LocaleContextHolder.getLocale());
                // 输出翻译后的文本
                gen.writeString(translated);
                return;
            } catch (NoSuchMessageException e) {
                // 找不到翻译时，降级输出原 Key
                gen.writeString(key);
                return;
            }
        }

        // 2. 普通字符串，原样输出
        gen.writeString(value);
    }
}