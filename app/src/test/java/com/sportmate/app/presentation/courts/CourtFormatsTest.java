package com.sportmate.app.presentation.courts;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.sportmate.app.R;
import com.sportmate.app.domain.model.CourtStatus;
import com.sportmate.app.domain.model.OperatingHour;
import com.sportmate.app.domain.model.Weekday;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class CourtFormatsTest {
    @Test
    public void byDayListsAllSevenDaysSundayFirstWithClosedDaysEmpty() {
        List<OperatingHour> hours = Arrays.asList(
                new OperatingHour(Weekday.MONDAY, "06:00", "12:00"),
                new OperatingHour(Weekday.MONDAY, "12:00", "14:00"),
                new OperatingHour(Weekday.SATURDAY, "08:00", "22:00"));

        List<CourtFormats.DayHours> days = CourtFormats.byDay(hours);

        assertEquals(7, days.size());
        assertEquals(Weekday.SUNDAY, days.get(0).day);
        assertTrue(days.get(0).periods.isEmpty());
        assertEquals(Weekday.MONDAY, days.get(1).day);
        // Touching periods stay separate, in backend order and unchanged.
        assertEquals(2, days.get(1).periods.size());
        assertEquals("06:00", days.get(1).periods.get(0).getOpensAt());
        assertEquals("12:00", days.get(1).periods.get(0).getClosesAt());
        assertEquals("12:00", days.get(1).periods.get(1).getOpensAt());
        assertEquals(Weekday.SATURDAY, days.get(6).day);
        assertEquals("22:00", days.get(6).periods.get(0).getClosesAt());
    }

    @Test
    public void byDayOfNoHoursIsSevenClosedDays() {
        List<CourtFormats.DayHours> days = CourtFormats.byDay(Collections.emptyList());

        assertEquals(7, days.size());
        for (CourtFormats.DayHours day : days) {
            assertTrue(day.periods.isEmpty());
        }
    }

    @Test
    public void labels() {
        assertEquals(R.string.day_sunday, CourtFormats.dayLabel(Weekday.SUNDAY));
        assertEquals(R.string.day_wednesday, CourtFormats.dayLabel(Weekday.WEDNESDAY));
        assertEquals(R.string.day_saturday, CourtFormats.dayLabel(Weekday.SATURDAY));
        assertEquals(R.string.court_status_active, CourtFormats.statusLabel(CourtStatus.ACTIVE));
        assertEquals(R.string.court_status_inactive, CourtFormats.statusLabel(CourtStatus.INACTIVE));
        assertEquals(R.string.value_unknown, CourtFormats.statusLabel(CourtStatus.UNKNOWN));
    }
}
