package top.enderliquid.audioflow.common.util.id;

import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

@Component
public class SnowflakeIdConverter implements IdConverter<Long> {

    @Override
    @Nullable
    public Long fromString(String s) {
        // 1. 基础非空校验
        if (s == null || s.isEmpty()) {
            return null;
        }
        try {
            // 2. 尝试解析
            long id = Long.parseLong(s);
            // 3. 雪花算法首位符号位固定为0，因此id应大于零
            if (id < 0) {
                return null;
            }
            return id;
        } catch (NumberFormatException e) {
            // 4. 捕获非数字格式或数值超出 Long 范围的异常
            return null;
        }
    }
}