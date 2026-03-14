package top.enderliquid.audioflow.common.util;

import org.slf4j.helpers.MessageFormatter;
/**
 * 字符串拼接工具
 * 底层复用 SLF4J 的 {} 占位符机制
 */
public class StrFormatter {
    /**
     * 使用 {} 格式化字符串
     */
    public static String format(String template, Object... args) {
        if (template == null || args == null || args.length == 0) {
            return template;
        }
        // 调用 SLF4J 的核心方法，并直接返回拼好的字符串
        return MessageFormatter.arrayFormat(template, args).getMessage();
    }
}