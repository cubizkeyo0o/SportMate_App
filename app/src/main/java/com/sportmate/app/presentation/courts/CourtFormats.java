package com.sportmate.app.presentation.courts;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import com.sportmate.app.R;
import com.sportmate.app.domain.model.CourtStatus;
import com.sportmate.app.domain.model.OperatingHour;
import com.sportmate.app.domain.model.Weekday;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Currency;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Display helpers for court discovery screens. */
final class CourtFormats {
    private CourtFormats() {
    }

    /** One day of the week and its periods (empty when the court is closed that day). */
    static final class DayHours {
        final Weekday day;
        final List<OperatingHour> periods;

        DayHours(Weekday day, List<OperatingHour> periods) {
            this.day = day;
            this.periods = periods;
        }
    }

    /**
     * Groups periods under all seven days, Sunday first as in the backend. Periods keep their
     * backend order and values; nothing is merged or converted.
     */
    @NonNull
    static List<DayHours> byDay(@NonNull List<OperatingHour> hours) {
        Map<Weekday, List<OperatingHour>> grouped = new EnumMap<>(Weekday.class);
        for (Weekday day : Weekday.values()) {
            grouped.put(day, new ArrayList<>());
        }
        for (OperatingHour hour : hours) {
            grouped.get(hour.getDay()).add(hour);
        }
        List<DayHours> result = new ArrayList<>(grouped.size());
        for (Map.Entry<Weekday, List<OperatingHour>> entry : grouped.entrySet()) {
            result.add(new DayHours(entry.getKey(), Collections.unmodifiableList(entry.getValue())));
        }
        return Collections.unmodifiableList(result);
    }

    /**
     * The amount with grouping, showing at least the currency's usual decimals (0 for VND, 2 for
     * USD) and never fewer than the value needs, so the backend amount is shown exactly.
     */
    @NonNull
    static String amount(@NonNull BigDecimal amount, @NonNull String currencyCode, @NonNull Locale locale) {
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

    @StringRes
    static int dayLabel(@NonNull Weekday day) {
        switch (day) {
            case SUNDAY:
                return R.string.day_sunday;
            case MONDAY:
                return R.string.day_monday;
            case TUESDAY:
                return R.string.day_tuesday;
            case WEDNESDAY:
                return R.string.day_wednesday;
            case THURSDAY:
                return R.string.day_thursday;
            case FRIDAY:
                return R.string.day_friday;
            case SATURDAY:
                return R.string.day_saturday;
            default:
                throw new IllegalArgumentException("unmapped day");
        }
    }

    @StringRes
    static int statusLabel(@NonNull CourtStatus status) {
        switch (status) {
            case ACTIVE:
                return R.string.court_status_active;
            case INACTIVE:
                return R.string.court_status_inactive;
            default:
                return R.string.value_unknown;
        }
    }
}
