package com.stowflow.inventra.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.Locale;

/** Formats monetary amounts in Moroccan dirhams (DH). */
public final class MoneyFormat {

    private static final NumberFormat AMOUNT = NumberFormat.getNumberInstance(Locale.FRENCH);

    static {
        AMOUNT.setMinimumFractionDigits(2);
        AMOUNT.setMaximumFractionDigits(2);
    }

    private MoneyFormat() {}

    public static String format(BigDecimal amount) {
        if (amount == null) {
            return "— DH";
        }
        BigDecimal scaled = amount.setScale(2, RoundingMode.HALF_UP);
        return AMOUNT.format(scaled).replace('\u202f', ' ') + " DH";
    }

    /** Compact thousands label for dashboard KPIs, e.g. "12,5 k DH". */
    public static String formatK(BigDecimal value) {
        if (value == null) {
            return "0 k DH";
        }
        BigDecimal k = value.divide(BigDecimal.valueOf(1000), 1, RoundingMode.HALF_UP);
        return String.format(Locale.FRENCH, "%.1f k DH", k.doubleValue()).replace('\u202f', ' ');
    }
}
