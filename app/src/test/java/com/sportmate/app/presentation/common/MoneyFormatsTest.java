package com.sportmate.app.presentation.common;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.math.BigDecimal;
import java.util.Locale;

public class MoneyFormatsTest {
    @Test
    public void amountUsesCurrencyDecimalsWithoutLosingPrecision() {
        assertEquals("150,000", MoneyFormats.amount(new BigDecimal("150000.00"), "VND", Locale.US));
        assertEquals("150,000.5", MoneyFormats.amount(new BigDecimal("150000.50"), "VND", Locale.US));
        assertEquals("12.50", MoneyFormats.amount(new BigDecimal("12.5"), "USD", Locale.US));
        assertEquals("0.00", MoneyFormats.amount(BigDecimal.ZERO, "USD", Locale.US));
        assertEquals("1,234.56", MoneyFormats.amount(new BigDecimal("1234.56"), "ZZZ", Locale.US));
        assertEquals("150.000", MoneyFormats.amount(new BigDecimal("150000"), "VND", Locale.GERMANY));
    }
}
