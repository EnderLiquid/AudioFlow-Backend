package top.enderliquid.audioflow.common.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class FractionTest {

    @Test
    void shouldConstructFractionFromIntegers() {
        Fraction fraction = new Fraction(3, 60);
        assertEquals(0.05, fraction.toDouble(), 0.0001);
    }

    @ParameterizedTest
    @CsvSource({
            "3/60, 0.05",
            "5/1, 5.0",
            "10/5, 2.0",
            "1/2, 0.5",
            "-3/60, -0.05"
    })
    void shouldParseFractionStringAndConvertToDouble(String fractionStr, double expected) {
        Fraction fraction = new Fraction(fractionStr);
        assertEquals(expected, fraction.toDouble(), 0.0001);
    }

    @Test
    void shouldThrowExceptionWhenDenominatorIsZeroFromIntegers() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new Fraction(3, 0)
        );
        assertEquals("分母不能为0", exception.getMessage());
    }

    @Test
    void shouldThrowExceptionWhenDenominatorIsZeroFromString() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new Fraction("3/0")
        );
        assertEquals("分母不能为0", exception.getMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "invalid",
            "3",
            "/60",
            "3/",
            "3/60/5",
            "3/",
            "abc/def"
    })
    void shouldThrowExceptionForInvalidFractionFormat(String invalidInput) {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new Fraction(invalidInput)
        );
        assertTrue(exception.getMessage().contains("分数格式错误") ||
                exception.getMessage().contains("不是有效的整数"));
    }

    @Test
    void shouldHandleWhitespaceInString() {
        Fraction fraction = new Fraction("  3  /  60  ");
        assertEquals(0.05, fraction.toDouble(), 0.0001);
    }

    @Test
    void shouldHandleNegativeNumeratorFromString() {
        Fraction fraction = new Fraction("-3/60");
        assertEquals(-0.05, fraction.toDouble(), 0.0001);
    }

    @Test
    void shouldHandleNegativeDenominatorFromString() {
        Fraction fraction = new Fraction("3/-60");
        assertEquals(-0.05, fraction.toDouble(), 0.0001);
    }
}
