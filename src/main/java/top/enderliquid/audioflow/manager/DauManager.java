package top.enderliquid.audioflow.manager;

import org.springframework.validation.annotation.Validated;

import java.time.LocalDate;

/**
 * 日活统计管理器
 * 使用Redis HyperLogLog进行日活的粗略统计
 */
@Validated
public interface DauManager {

    /**
     * 记录用户日活
     *
     * @param userId 用户ID
     * @param date   日期
     */
    void recordDau(Long userId, LocalDate date);

    /**
     * 获取指定日期的日活数
     *
     * @param date 日期
     * @return 日活数，如果键不存在返回0
     */
    long getDauCount(LocalDate date);
}