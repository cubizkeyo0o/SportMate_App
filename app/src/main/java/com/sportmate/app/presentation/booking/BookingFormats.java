package com.sportmate.app.presentation.booking;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import com.sportmate.app.R;
import com.sportmate.app.domain.model.BookingStatus;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/** Display helpers for bookings. */
final class BookingFormats {
    private static final BigDecimal MINUTES_PER_HOUR = BigDecimal.valueOf(60);

    private BookingFormats() {
    }

    /**
     * An estimate of the booking price using the backend's documented rule (price per hour times
     * minutes / 60, rounded to 2 decimals with halves away from zero). Only a preview: the backend
     * calculates the real total when the booking is created.
     */
    @NonNull
    static BigDecimal estimate(@NonNull BigDecimal pricePerHour, long minutes) {
        // Prices are never negative, so HALF_UP is "away from zero".
        return pricePerHour.multiply(BigDecimal.valueOf(minutes)).divide(MINUTES_PER_HOUR, 2, RoundingMode.HALF_UP);
    }

    /** A court-local {@code "yyyy-MM-dd"} as a readable date, e.g. "Monday, October 5, 2026". */
    @NonNull
    static String date(@NonNull String localDate, int style, @NonNull Locale locale) {
        // The date carries no time zone; parse and format it in UTC so it never shifts.
        SimpleDateFormat parser = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        parser.setTimeZone(TimeZone.getTimeZone("UTC"));
        DateFormat output = DateFormat.getDateInstance(style, locale);
        output.setTimeZone(TimeZone.getTimeZone("UTC"));
        try {
            return output.format(parser.parse(localDate));
        } catch (ParseException e) {
            return localDate;
        }
    }

    /** A UTC instant as date and 24-hour time in the court's zone, e.g. "Oct 5, 2026 14:35". */
    @NonNull
    static String courtDateTime(long utcMillis, @NonNull String timeZoneId, @NonNull Locale locale) {
        TimeZone zone = TimeZone.getTimeZone(timeZoneId);
        DateFormat date = DateFormat.getDateInstance(DateFormat.MEDIUM, locale);
        date.setTimeZone(zone);
        SimpleDateFormat time = new SimpleDateFormat("HH:mm", locale);
        time.setTimeZone(zone);
        Date instant = new Date(utcMillis);
        return date.format(instant) + " " + time.format(instant);
    }

    @StringRes
    static int statusLabel(@NonNull BookingStatus status) {
        switch (status) {
            case PENDING:
                return R.string.booking_status_pending;
            case CONFIRMED:
                return R.string.booking_status_confirmed;
            case CANCELLED:
                return R.string.booking_status_cancelled;
            case COMPLETED:
                return R.string.booking_status_completed;
            case NO_SHOW:
                return R.string.booking_status_no_show;
            default:
                return R.string.value_unknown;
        }
    }
}
