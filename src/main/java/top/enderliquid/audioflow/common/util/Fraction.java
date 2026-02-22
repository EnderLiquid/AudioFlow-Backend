package top.enderliquid.audioflow.common.util;

public class Fraction {
    private final int numerator;   // 分子
    private final int denominator; // 分母

    /**
     * 构造一个分数对象（整数参数）
     *
     * @param numerator   分子
     * @param denominator 分母（不能为0）
     * @throws IllegalArgumentException 如果分母为0
     */
    public Fraction(int numerator, int denominator) {
        if (denominator == 0) {
            throw new IllegalArgumentException("分母不能为0");
        }
        this.numerator = numerator;
        this.denominator = denominator;
    }

    /**
     * 构造一个分数对象（字符串参数，格式如 "3/60"）
     *
     * @param fractionStr 分数字符串，格式为 "分子/分母"
     * @throws IllegalArgumentException 如果字符串格式错误或分母为0
     */
    public Fraction(String fractionStr) {
        // 去除字符串首尾空格
        String trimmed = fractionStr.trim();
        // 按 "/" 分割
        String[] parts = trimmed.split("/");
        if (parts.length != 2) {
            throw new IllegalArgumentException("分数格式错误，应为 '分子/分母'");
        }
        try {
            // 解析分子和分母（允许正负号）
            this.numerator = Integer.parseInt(parts[0].trim());
            this.denominator = Integer.parseInt(parts[1].trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("分子或分母不是有效的整数", e);
        }
        if (this.denominator == 0) {
            throw new IllegalArgumentException("分母不能为0");
        }
    }

    /**
     * 将分数转换为 double 值
     *
     * @return 分数的浮点表示
     */
    public double toDouble() {
        return (double) numerator / denominator;
    }
}