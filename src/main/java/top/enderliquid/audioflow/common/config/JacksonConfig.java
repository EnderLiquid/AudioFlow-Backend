package top.enderliquid.audioflow.common.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

@Configuration
public class JacksonConfig {
    //自动Trim非Get方法传入的字符串，字符串为空则设为null
    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        // 创建一个模块
        SimpleModule module = new SimpleModule();
        // 注册自定义的 String 反序列化器
        module.addDeserializer(String.class, new JsonDeserializer<String>() {
            @Override
            public String deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
                String result = p.getText();
                if (result == null || result.isBlank()) return null;
                return result.trim();
            }
        });
        objectMapper.registerModule(module);
        return objectMapper;
    }
}