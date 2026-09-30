package com.sportmate.app.presentation.booking;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.sportmate.app.R;
import com.sportmate.app.domain.model.OperatingHour;
import com.sportmate.app.domain.model.Weekday;
import com.sportmate.app.presentation.common.UiText;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.TimeZone;

public class BookingTimesTest {
    private static final TimeZone HCM = TimeZone.getTimeZone("Asia/Ho_Chi_Minh");
    private static final long START_UTC = 4_094_852_400_000L; // 2099-10-05T03:00:00Z = 10:00 in Ho Chi Minh City
    private static final long NOW = START_UTC - 24 * 3_600_000L;

    // 2099-10-05 is a Monday.
    private static final List<OperatingHour> HOURS = Arrays.asList(
            new OperatingHour(Weekday.MONDAY, "06:00", "12:00"),
            new OperatingHour(Weekday.MONDAY, "12:00", "14:00"),
            new OperatingHour(Weekday.MONDAY, "17:00", "22:00"));

    @Test
    public void courtLocalTimeUsesTheCourtZoneNotThePhones() {
        TimeZone original = TimeZone.getDefault();
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("America/Los_Angeles"));
            assertEquals(Long.valueOf(START_UTC), BookingTimes.toInstant("2099-10-05", "10:00", HCM));
        } finally {
            TimeZone.setDefault(original);
        }
    }

    @Test
    public void localTimeSkippedByDaylightSavingDoesNotExist() {
        TimeZone newYork = TimeZone.getTimeZone("America/New_York");
        // Clocks jump from 02:00 to 03:00 on 2026-03-08.
        assertNull(BookingTimes.toInstant("2026-03-08", "02:30", newYork));
        assertTrue(BookingTimes.toInstant("2026-03-08", "03:30", newYork) != null);
    }

    @Test
    public void weekdayOfPlainDates() {
        assertEquals(Weekday.MONDAY, BookingTimes.weekdayOf("2099-10-05"));
        assertEquals(Weekday.SUNDAY, BookingTimes.weekdayOf("2026-10-04"));
    }

    @Test
    public void todayIsTheCourtsDate() {
        // 2099-10-04T20:00Z is already 2099-10-05 03:00 in Ho Chi Minh City.
        assertEquals("2099-10-05", BookingTimes.today(HCM, START_UTC - 7 * 3_600_000L));
        assertEquals("2099-10-04", BookingTimes.today(TimeZone.getTimeZone("UTC"), START_UTC - 7 * 3_600_000L));
    }

    @Test
    public void touchingPeriodsAreContinuousButGapsAreNot() {
        assertTrue(BookingTimes.withinPeriods(HOURS, "10:00", "13:00"));
        assertTrue(BookingTimes.withinPeriods(HOURS, "06:00", "14:00"));
        assertTrue(BookingTimes.withinPeriods(HOURS, "17:00", "22:00"));
        assertFalse(BookingTimes.withinPeriods(HOURS, "13:00", "18:00"));
        assertFalse(BookingTimes.withinPeriods(HOURS, "05:30", "07:00"));
        assertFalse(BookingTimes.withinPeriods(HOURS, "21:00", "22:30"));
        assertFalse(BookingTimes.withinPeriods(Collections.emptyList(), "10:00", "11:00"));
    }

    @Test
    public void validSelectionPasses() {
        assertNull(BookingTimes.validate(selection("cs-1", "2099-10-05", "10:00", "11:30"), HCM, HOURS, NOW));
    }

    @Test
    public void validationReportsTheFirstObviousProblem() {
        assertEquals(UiText.of(R.string.booking_error_choose_sport),
                BookingTimes.validate(selection(null, "2099-10-05", "10:00", "11:00"), HCM, HOURS, NOW));
        assertEquals(UiText.of(R.string.booking_error_choose_date),
                BookingTimes.validate(selection("cs-1", null, "10:00", "11:00"), HCM, HOURS, NOW));
        assertEquals(UiText.of(R.string.booking_error_choose_times),
                BookingTimes.validate(selection("cs-1", "2099-10-05", "10:00", null), HCM, HOURS, NOW));
        assertEquals(UiText.of(R.string.booking_error_end_after_start),
                BookingTimes.validate(selection("cs-1", "2099-10-05", "11:00", "11:00"), HCM, HOURS, NOW));
        assertEquals(UiText.of(R.string.booking_error_start_in_future),
                BookingTimes.validate(selection("cs-1", "2099-10-05", "10:00", "11:00"), HCM, HOURS, START_UTC));
        assertEquals(UiText.of(R.string.booking_error_closed_day),
                BookingTimes.validate(selection("cs-1", "2099-10-06", "10:00", "11:00"), HCM, HOURS, NOW));
        assertEquals(UiText.of(R.string.booking_error_outside_hours),
                BookingTimes.validate(selection("cs-1", "2099-10-05", "13:00", "18:00"), HCM, HOURS, NOW));
        assertEquals(UiText.of(R.string.booking_error_time_not_exist),
                BookingTimes.validate(selection("cs-1", "2026-03-08", "02:30", "04:00"),
                        TimeZone.getTimeZone("America/New_York"), HOURS, 0L));
    }

    private static BookingSelection selection(String courtSportId, String date, String start, String end) {
        return new BookingSelection(courtSportId, date, start, end);
    }
}
