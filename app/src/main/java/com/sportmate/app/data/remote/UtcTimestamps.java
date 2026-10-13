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
            Pattern.compile("^(\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2})(\\.(\\d{1,7}))?(Z|[+-]\\d{2}:\\d{2})?$");

    private UtcTimestamps() {
    }

    /** Epoch milliseconds of a backend UTC instant; throws IllegalStateException if unreadable. */
    public static long parse(@Nullable String value) {
        return parse(value, false);
    }

    /** Epoch milliseconds of an ISO-8601 DateTimeOffset returned by the Play Request API. */
    public static long parseDateTimeOffset(@Nullable String value) {
        return parse(value, true);
    }

    private static long parse(@Nullable String value, boolean allowOffset) {
        Matcher matcher = value == null ? null : INSTANT.matcher(value);
        if (matcher == null || !matcher.matches()) {
            throw new IllegalStateException("not a UTC timestamp");
        }
        try {
            String fraction = matcher.group(3);
            String zone = matcher.group(4);
            if (!allowOffset && zone != null && !"Z".equals(zone)) {
                throw new IllegalStateException("not a UTC timestamp");
            }
            String normalized = matcher.group(1)
                    + (fraction == null ? "" : "." + (fraction + "000").substring(0, 3))
                    + (zone == null || "Z".equals(zone) ? "+00:00" : zone);
            String pattern = fraction == null
                    ? "yyyy-MM-dd'T'HH:mm:ssXXX"
                    : "yyyy-MM-dd'T'HH:mm:ss.SSSXXX";
            return format(pattern).parse(normalized).getTime();
        } catch (IllegalStateException e) {
            throw e;
        } catch (ParseException e) {
            throw new IllegalStateException("not a UTC timestamp", e);
        }
    }

    /** ISO-8601 in UTC with {@code Z}, as accepted by POST api/v1/bookings. */
    @NonNull
    public static String format(long epochMillis) {
        SimpleDateFormat format = format("yyyy-MM-dd'T'HH:mm:ss");
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        return format.format(new Date(epochMillis)) + "Z";
    }

    // SimpleDateFormat is not thread-safe; a new instance per call is cheap enough here.
    private static SimpleDateFormat format(String pattern) {
        SimpleDateFormat format = new SimpleDateFormat(pattern, Locale.US);
        format.setLenient(false);
        return format;
    }
}
