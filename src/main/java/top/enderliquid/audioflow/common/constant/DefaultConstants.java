package top.enderliquid.audioflow.common.constant;

/**
 * 业务默认值常量
 */
public final class DefaultConstants {
    private DefaultConstants() {
    }

    // 分页相关
    /**
     * 默认页码
     */
    public static final long PAGE_DEFAULT_INDEX = 1L;

    /**
     * 默认每页大小
     */
    public static final long PAGE_DEFAULT_SIZE = 10L;

    /**
     * 默认排序方式（false=倒序）
     */
    public static final boolean PAGE_DEFAULT_ASC = false;

    // Redis统计数据过期时间
    /**
     * 统计数据Redis key过期天数（日活、日签到数等）
     */
    public static final int DAILY_STATS_REDIS_EXPIRE_DAYS = 7;
}