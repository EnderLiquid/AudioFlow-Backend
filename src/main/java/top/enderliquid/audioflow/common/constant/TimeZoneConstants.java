package top.enderliquid.audioflow.common.constant;

public class TimeZoneConstants {
    /**
     * 系统全局时区标识：亚洲/上海 (中国标准时间)
     * <p>
     * <b>为什么使用 "Asia/Shanghai" 而不是 "GMT+8" 或 "UTC+8"？</b>
     * <ul>
     *     <li>
     *         <b>1. 概念本质的区别：</b><br>
     *         {@code GMT+8} (固定偏移量 ZoneOffset)：表示永远比格林威治时间快 8 小时，它是死板的数学计算，不包含任何地方法规和历史信息。<br>
     *         {@code Asia/Shanghai} (地理时区 ZoneRegion)：属于 IANA 时区库定义的地理概念，它代表“在上海/中国大陆执行的法定时间规则”。
     *     </li>
     *     <li>
     *         <b>2. 历史数据的准确性（夏令时问题）：</b><br>
     *         中国在 1986 年至 1991 年间实行过夏令时(DST)。如果处理那段历史期间的数据（例如：1988年7月1日），
     *         使用 {@code Asia/Shanghai}，底层 API 会自动识别出当时是夏令时，将偏移量调整为 +9 小时；
     *         而使用 {@code GMT+8} 则永远是 +8 小时，会导致历史数据出现 1 小时的偏差。
     *     </li>
     *     <li>
     *         <b>3. 面向未来的扩展性：</b><br>
     *         时区规则是由当地政府决定的，未来如果政策变动（如再次实行夏令时），只要升级 Java 运行环境或系统的 IANA 时区数据库，
     *         使用 {@code Asia/Shanghai} 的系统就能自动适应新规则，而硬编码的 {@code GMT+8} 则必须改代码。
     *     </li>
     * </ul>
     */
    public static final String GLOBAL_TIME_ZONE_ID = "Asia/Shanghai";
}
