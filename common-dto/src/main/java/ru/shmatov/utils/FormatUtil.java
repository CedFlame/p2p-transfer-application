package ru.shmatov.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class FormatUtil {
    public static String formatBalance(long littleValue) {
        BigDecimal bigValue = BigDecimal.valueOf(littleValue)
                .divide(BigDecimal.valueOf(100));
        return bigValue.setScale(2, RoundingMode.DOWN).toString();
    }
}
