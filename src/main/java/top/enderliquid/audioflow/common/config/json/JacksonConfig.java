package top.enderliquid.audioflow.common.config.json;

import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.format.DateTimeFormatter;
import java.util.TimeZone;

import static top.enderliquid.audioflow.common.constant.TimeZoneConstants.GLOBAL_TIME_ZONE_ID;

@Configuration
public class JacksonConfig {
    @Autowired
    private MessageSource messageSource;

    // 自动 Trim JSON 表单 (application/json) 请求传入的字符串
    // 字符串为空则设为 null
    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jacksonTrimmingCustomizer() {
        return builder -> {
            // 自动 trim 字符串
            builder.deserializerByType(String.class, new StringTrimmingDeserializer());

            // i18n
            builder.serializerByType(String.class, new I18nStringSerializer(messageSource));

            // 时区
            // 1. 格式化 LocalDateTime
            // 默认为 ISO-8601 标准，如 "2023-10-25T10:00:00"
            // 修改后去除了字符串中间的 "T"，如 "2023-10-25 10:00:00"
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            builder.serializers(new LocalDateTimeSerializer(formatter));
            builder.deserializers(new LocalDateTimeDeserializer(formatter));
            // 2. 统一时区
            // JacksonConfig 默认时区被硬编码为 UTC
            // 带有时区属性的类的实例，如 Date, Calendar, ZonedDateTime
            // 会在（反）序列化时转换时区到 UTC 并丢失时区信息，导致时间偏移
            // 因此需要显式配置 Jackson 时区
            builder.timeZone(TimeZone.getTimeZone(GLOBAL_TIME_ZONE_ID));

            // Long 自动转为 String，防止前端序列化丢失精度
            builder.serializerByType(Long.class, ToStringSerializer.instance);
        };
    }
}
