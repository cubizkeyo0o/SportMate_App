package com.sportmate.app.data.remote;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The backend's UTC instants on the wire. The backend writes {@code *_utc} fields as ISO-8601 with
 * {@code Z} and optional fractional seconds; booking requests are sent as whole-minute UTC instants.
 * Implemented without java.time, which is unavailable on the app's minimum API level.
 */
public final class UtcTimestamps {
    private static final Pattern INSTANT =
            Pattern.compile("^(\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2})(\\.(\\d{1,7}))?Z?$");
    private static final String PATTERN = "yyyy-MM-dd'T'HH:mm:ss";

    private UtcTimestamps() {
    }

    /** Epoch milliseconds of a backend UTC instant; throws IllegalStateException if unreadable. */
    public static long parse(@Nullable String value) {
        Matcher matcher = value == null ? null : INSTANT.matcher(value);
        if (matcher == null || !matcher.matches()) {
            throw new IllegalStateException("not a UTC timestamp");
        }
        try {
            long millis = format().parse(matcher.group(1)).getTime();
            String fraction = matcher.group(3);
            if (fraction != null) {
                millis += Long.parseLong((fraction + "000").substring(0, 3));
            }
            return millis;
        } catch (ParseException e) {
            throw new IllegalStateException("not a UTC timestamp", e);
        }
    }

    /** ISO-8601 in UTC with {@code Z}, as accepted by POST api/v1/bookings. */
    @NonNull
    public static String format(long epochMillis) {
        return format().format(new Date(epochMillis)) + "Z";
    }

    // SimpleDateFormat is not thread-safe; a new instance per call is cheap enough here.
    private static SimpleDateFormat format() {
        SimpleDateFormat format = new SimpleDateFormat(PATTERN, Locale.US);
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        format.setLenient(false);
        return format;
    }
}
