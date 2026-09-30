package com.sportmate.app.presentation.booking;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.sportmate.app.R;
import com.sportmate.app.domain.model.Booking;
import com.sportmate.app.domain.model.BookingStatus;
import com.sportmate.app.testutil.ControllableBookingRepository;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class BookingGroupsTest {
    private static final long HOUR = 3_600_000L;
    private static final long NOW = 4_094_852_400_000L;

    @Test
    public void upcomingSoonestFirstThenPastMostRecentFirst() {
        Booking later = booking("later", BookingStatus.CONFIRMED, NOW + 48 * HOUR);
        Booking soon = booking("soon", BookingStatus.PENDING, NOW + 2 * HOUR);
        Booking ongoing = booking("ongoing", BookingStatus.CONFIRMED, NOW - HOUR / 2);
        Booking cancelledFuture = booking("cancelled", BookingStatus.CANCELLED, NOW + 24 * HOUR);
        Booking ended = booking("ended", BookingStatus.CONFIRMED, NOW - 5 * HOUR);
        Booking completed = booking("completed", BookingStatus.COMPLETED, NOW - 48 * HOUR);

        List<BookingGroups.Row> rows = BookingGroups.rows(
                Arrays.asList(completed, later, ended, soon, cancelledFuture, ongoing), NOW);

        assertEquals(R.string.my_bookings_upcoming, rows.get(0).header);
        assertEquals("ongoing", rows.get(1).booking.getId());
        assertEquals("soon", rows.get(2).booking.getId());
        assertEquals("later", rows.get(3).booking.getId());
        assertEquals(R.string.my_bookings_past, rows.get(4).header);
        assertEquals("cancelled", rows.get(5).booking.getId());
        assertEquals("ended", rows.get(6).booking.getId());
        assertEquals("completed", rows.get(7).booking.getId());
        assertEquals(8, rows.size());
    }

    @Test
    public void emptySectionsAreOmitted() {
        List<BookingGroups.Row> onlyPast = BookingGroups.rows(
                Collections.singletonList(booking("done", BookingStatus.COMPLETED, NOW - 48 * HOUR)), NOW);

        assertEquals(2, onlyPast.size());
        assertEquals(R.string.my_bookings_past, onlyPast.get(0).header);
        assertTrue(BookingGroups.rows(Collections.emptyList(), NOW).isEmpty());
    }

    // A one-hour booking starting at the given instant.
    private static Booking booking(String id, BookingStatus status, long start) {
        return ControllableBookingRepository.booking(id, status, start, start + HOUR);
    }
}
