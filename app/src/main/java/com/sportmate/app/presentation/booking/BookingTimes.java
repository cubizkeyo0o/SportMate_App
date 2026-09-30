package com.sportmate.app.presentation.booking;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.sportmate.app.R;
import com.sportmate.app.domain.model.OperatingHour;
import com.sportmate.app.domain.model.Weekday;
import com.sportmate.app.presentation.common.UiText;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Court-local booking times. The user picks wall-clock values of the court; they are converted to
 * instants with the court's IANA time zone, never the phone's. The checks here only catch obvious
 * mistakes early; the backend applies the authoritative booking rules.
 */
final class BookingTimes {
    private static final String DATE_PATTERN = "yyyy-MM-dd";
    private static final String DATE_TIME_PATTERN = "yyyy-MM-dd HH:mm";

    private BookingTimes() {
    }

    /**
     * The instant of a court-local date and time, or null if that local time does not exist in the
     * court's zone (skipped by a daylight-saving change).
     */
    @Nullable
    static Long toInstant(@NonNull String date, @NonNull String time, @NonNull TimeZone zone) {
        SimpleDateFormat format = format(DATE_TIME_PATTERN, zone);
        String local = date + " " + time;
        try {
            Date instant = format.parse(local);
            // A skipped local time parses to a different wall-clock time; reject it.
            return local.equals(format.format(instant)) ? instant.getTime() : null;
        } catch (ParseException e) {
            return null;
        }
    }

    /** Today's date at the court, {@code "yyyy-MM-dd"}. */
    @NonNull
    static String today(@NonNull TimeZone zone, long nowMillis) {
        return format(DATE_PATTERN, zone).format(new Date(nowMillis));
    }

    /** The weekday of a calendar date (independent of any time zone). */
    @NonNull
    static Weekday weekdayOf(@NonNull String date) {
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"), Locale.US);
        try {
            calendar.setTime(format(DATE_PATTERN, TimeZone.getTimeZone("UTC")).parse(date));
        } catch (ParseException e) {
            throw new IllegalArgumentException("not a date", e);
        }
        // Calendar.SUNDAY is 1; Weekday is Sunday first like the backend.
        return Weekday.values()[calendar.get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY];
    }

    /** The day's periods, in backend order. */
    @NonNull
    static List<OperatingHour> periodsOn(@NonNull List<OperatingHour> hours, @NonNull Weekday day) {
        List<OperatingHour> periods = new ArrayList<>();
        for (OperatingHour hour : hours) {
            if (hour.getDay() == day) {
                periods.add(hour);
            }
        }
        return Collections.unmodifiableList(periods);
    }

    /**
     * Whether [start, end] lies inside the union of the periods; touching periods count as
     * continuous. "HH:mm" strings compare correctly as text.
     */
    static boolean withinPeriods(@NonNull List<OperatingHour> periods, @NonNull String start, @NonNull String end) {
        List<OperatingHour> sorted = new ArrayList<>(periods);
        Collections.sort(sorted, (a, b) -> a.getOpensAt().compareTo(b.getOpensAt()));
        String blockStart = null;
        String blockEnd = null;
        for (OperatingHour period : sorted) {
            if (blockEnd != null && period.getOpensAt().compareTo(blockEnd) <= 0) {
                if (period.getClosesAt().compareTo(blockEnd) > 0) {
                    blockEnd = period.getClosesAt();
                }
                continue;
            }
            if (covers(blockStart, blockEnd, start, end)) {
                return true;
            }
            blockStart = period.getOpensAt();
            blockEnd = period.getClosesAt();
        }
        return covers(blockStart, blockEnd, start, end);
    }

    /** The first obvious problem with the selection, or null if it can be sent to the backend. */
    @Nullable
    static UiText validate(
            @NonNull BookingSelection selection,
            @NonNull TimeZone zone,
            @NonNull List<OperatingHour> hours,
            long nowMillis) {
        if (selection.courtSportId == null) {
            return UiText.of(R.string.booking_error_choose_sport);
        }
        if (selection.date == null) {
            return UiText.of(R.string.booking_error_choose_date);
        }
        if (selection.startTime == null || selection.endTime == null) {
            return UiText.of(R.string.booking_error_choose_times);
        }
        if (selection.startTime.compareTo(selection.endTime) >= 0) {
            return UiText.of(R.string.booking_error_end_after_start);
        }
        Long start = toInstant(selection.date, selection.startTime, zone);
        Long end = toInstant(selection.date, selection.endTime, zone);
        if (start == null || end == null) {
            return UiText.of(R.string.booking_error_time_not_exist);
        }
        if (start <= nowMillis) {
            return UiText.of(R.string.booking_error_start_in_future);
        }
        List<OperatingHour> periods = periodsOn(hours, weekdayOf(selection.date));
        if (periods.isEmpty()) {
            return UiText.of(R.string.booking_error_closed_day);
        }
        if (!withinPeriods(periods, selection.startTime, selection.endTime)) {
            return UiText.of(R.string.booking_error_outside_hours);
        }
        return null;
    }

    private static boolean covers(@Nullable String blockStart, @Nullable String blockEnd, String start, String end) {
        return blockStart != null && blockStart.compareTo(start) <= 0 && blockEnd.compareTo(end) >= 0;
    }

    private static SimpleDateFormat format(String pattern, TimeZone zone) {
        SimpleDateFormat format = new SimpleDateFormat(pattern, Locale.US);
        format.setTimeZone(zone);
        format.setLenient(false);
        return format;
    }
}
