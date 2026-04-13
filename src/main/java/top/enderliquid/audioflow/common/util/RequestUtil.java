package top.enderliquid.audioflow.common.util;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import nl.basjes.parse.useragent.UserAgent;
import nl.basjes.parse.useragent.UserAgentAnalyzer;
import org.springframework.lang.Nullable;
import top.enderliquid.audioflow.dto.request.session.LoginContextDTO;

/**
 * 请求工具类
 * 提供获取客户端 IP、设备类型等请求相关信息的方法
 */
@Slf4j
public final class RequestUtil {

    /**
     * 私有构造函数，防止实例化
     */
    private RequestUtil() {
        throw new UnsupportedOperationException("工具类不能被实例化");
    }

    /**
     * 获取客户端真实 IP 地址
     * <p>
     * 优先级：
     * 1. X-Real-IP 头（Nginx 等反向代理设置）
     * 2. X-Forwarded-For 头的第一个 IP
     * 3. request.getRemoteAddr()
     *
     * @param request HTTP 请求对象
     * @return 客户端 IP 地址
     */
    public static String getClientIp(HttpServletRequest request) {
        // 优先从 X-Real-IP 头获取
        String ip = getHeaderIfNotBlank(request, "X-Real-IP");

        // 其次从 X-Forwarded-For 头获取（取第一个 IP）
        if (ip == null) {
            ip = getHeaderIfNotBlank(request, "X-Forwarded-For");
            if (ip != null) {
                // X-Forwarded-For 可能包含多个 IP，取第一个
                ip = ip.split(",")[0].trim();
            }
        }

        // 最后使用 RemoteAddr
        if (ip == null) {
            ip = request.getRemoteAddr();
        }

        return ip;
    }

    /**
     * 获取客户端设备类型
     * <p>
     * 使用 Yauaa 库解析 User-Agent 头，返回设备类型如：
     * Desktop, Mobile, Tablet, Phone, Game Console, Smart TV 等
     *
     * @param request HTTP 请求对象
     * @return 设备类型，如果无法解析则返回 null
     */
    public static String getDeviceType(HttpServletRequest request) {
        String userAgentHeader = getUserAgent(request);
        if (userAgentHeader == null) return null;

        try {
            UserAgentAnalyzer analyzer = UserAgentAnalyzerHolder.INSTANCE;
            UserAgent userAgent = analyzer.parse(userAgentHeader);
            return userAgent.getValue("DeviceClass");
        } catch (Exception e) {
            log.warn("解析 User-Agent 失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 获取客户端浏览器信息
     * <p>
     * 使用 Yauaa 库解析 User-Agent 头，返回浏览器名称和版本如：
     * Chrome 103.0.5060.134, Firefox 102.0, Safari 15.6 等
     *
     * @param request HTTP 请求对象
     * @return 浏览器名称和版本，如果无法解析则返回 null
     */
    public static String getBrowser(HttpServletRequest request) {
        String userAgentHeader = getUserAgent(request);
        if (userAgentHeader == null) return null;

        try {
            UserAgentAnalyzer analyzer = UserAgentAnalyzerHolder.INSTANCE;
            UserAgent userAgent = analyzer.parse(userAgentHeader);
            return userAgent.getValue("AgentNameVersion");
        } catch (Exception e) {
            log.warn("解析 User-Agent 失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 获取客户端操作系统信息
     * <p>
     * 使用 Yauaa 库解析 User-Agent 头，返回操作系统名称和版本如：
     * Windows 10, macOS 12.5, Linux, Android 13, iOS 16.5 等
     *
     * @param request HTTP 请求对象
     * @return 操作系统名称和版本，如果无法解析则返回 null
     */
    public static String getOs(HttpServletRequest request) {
        String userAgentHeader = getUserAgent(request);
        if (userAgentHeader == null) return null;

        try {
            UserAgentAnalyzer analyzer = UserAgentAnalyzerHolder.INSTANCE;
            UserAgent userAgent = analyzer.parse(userAgentHeader);
            return userAgent.getValue("OperatingSystemNameVersion");
        } catch (Exception e) {
            log.warn("解析 User-Agent 失败: {}", e.getMessage());
            return null;
        }
    }

    public static String getUserAgent(HttpServletRequest request) {
        return getHeaderIfNotBlank(request, "User-Agent");
    }

    /**
     * 构建登录上下文，封装设备信息
     * <p>
     * 从请求中提取 IP、设备类型、操作系统、浏览器和 User-Agent 信息
     *
     * @param request HTTP 请求对象
     * @return 登录上下文对象
     */
    public static LoginContextDTO getLoginContext(HttpServletRequest request) {
        return new LoginContextDTO(
                getClientIp(request),
                getDeviceType(request),
                getOs(request),
                getBrowser(request),
                getUserAgent(request)
        );
    }

    @Nullable
    public static String getHeaderIfNotBlank(HttpServletRequest request, String name) {
        String header = request.getHeader(name);
        if (header == null || header.isBlank()) return null;
        return header.trim();
    }

    /**
     * 静态内部类实现单例模式
     * UserAgentAnalyzer 初始化较耗时，使用懒加载单例
     */
    private static class UserAgentAnalyzerHolder {
        // 限制输出字段以提高性能
        private static final UserAgentAnalyzer INSTANCE = UserAgentAnalyzer
                .newBuilder()
                .withField("DeviceClass")
                .withField("AgentNameVersion")
                .withField("OperatingSystemNameVersion")
                .build();
    }
}