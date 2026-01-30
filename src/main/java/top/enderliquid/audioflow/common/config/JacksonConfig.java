package top.enderliquid.audioflow.common.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

@Configuration
public class JacksonConfig {
    // 自动 Trim JSON 表单 (application/json) 请求传入的字符串
    // 字符串为空则设为 null
    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jacksonTrimmingCustomizer() {
        return builder -> {
            builder.deserializerByType(String.class, new JsonDeserializer<String>() {
                @Override
                public String deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
                    String result = p.getText();
                    if (result == null || result.isBlank()) {
                        return null;
                    }
                    return result.trim();
                }
            });
        };
    }
}
