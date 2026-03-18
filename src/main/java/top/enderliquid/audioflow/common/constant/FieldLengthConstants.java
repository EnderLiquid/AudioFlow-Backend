package top.enderliquid.audioflow.common.constant;

/**
 * 字段长度常量
 */
public final class FieldLengthConstants {
    private FieldLengthConstants() {
    }

    // 密码
    public static final int PASSWORD_MIN = 6;
    public static final int PASSWORD_MAX = 64;

    // 邮箱
    public static final int EMAIL_MIN = 1;
    public static final int EMAIL_MAX = 128;

    // 用户名
    public static final int USER_NAME_MIN = 1;
    public static final int USER_NAME_MAX = 64;

    // 歌曲名称
    public static final int SONG_NAME_MIN = 1;
    public static final int SONG_NAME_MAX = 64;

    // 歌曲描述
    public static final int SONG_DESCRIPTION_MIN = 1;
    public static final int SONG_DESCRIPTION_MAX = 128;

    // 登录流水字段
    public static final int LOGIN_LOG_IP_MAX = 45;
    public static final int LOGIN_LOG_DEVICE_TYPE_MAX = 50;
    public static final int LOGIN_LOG_OS_MAX = 100;
    public static final int LOGIN_LOG_BROWSER_MAX = 100;
    public static final int LOGIN_LOG_USER_AGENT_MAX = 512;
}