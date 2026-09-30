package com.sportmate.app.presentation.common;

import androidx.annotation.NonNull;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Currency;
import java.util.Locale;

/** Display of backend money amounts. */
public final class MoneyFormats {
    private MoneyFormats() {
    }

    /**
     * The amount with grouping, showing at least the currency's usual decimals (0 for VND, 2 for
     * USD) and never fewer than the value needs, so the backend amount is shown exactly.
     */
    @NonNull
    public static String amount(@NonNull BigDecimal amount, @NonNull String currencyCode, @NonNull Locale locale) {
        int digits = Math.max(0, amount.stripTrailingZeros().scale());
        try {
            digits = Math.max(digits, Currency.getInstance(currencyCode).getDefaultFractionDigits());
        } catch (IllegalArgumentException unknownCurrency) {
            // Not a code this device knows; show the value's own decimals.
        }
        NumberFormat format = NumberFormat.getNumberInstance(locale);
        format.setGroupingUsed(true);
        format.setMinimumFractionDigits(digits);
        format.setMaximumFractionDigits(digits);
        return format.format(amount);
    }
}
