package com.sportmate.app.presentation.booking;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import com.sportmate.app.R;
import com.sportmate.app.domain.model.Booking;
import com.sportmate.app.domain.model.BookingStatus;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Splits the user's bookings into upcoming and past sections for the My Bookings list. */
final class BookingGroups {
    /** A section header or a booking. */
    static final class Row {
        @StringRes
        final int header;
        @Nullable
        final Booking booking;

        private Row(@StringRes int header, @Nullable Booking booking) {
            this.header = header;
            this.booking = booking;
        }

        static Row header(@StringRes int header) {
            return new Row(header, null);
        }

        static Row booking(@NonNull Booking booking) {
            return new Row(0, booking);
        }

        boolean isHeader() {
            return booking == null;
        }
    }

    private BookingGroups() {
    }

    /**
     * Upcoming: Pending or Confirmed bookings that have not ended, soonest first. Past: everything
     * else (ended, cancelled, completed, no-show), most recent first. Empty sections are omitted.
     */
    @NonNull
    static List<Row> rows(@NonNull List<Booking> bookings, long nowMillis) {
        List<Booking> upcoming = new ArrayList<>();
        List<Booking> past = new ArrayList<>();
        for (Booking booking : bookings) {
            boolean active = booking.getStatus() == BookingStatus.PENDING
                    || booking.getStatus() == BookingStatus.CONFIRMED;
            if (active && booking.getEndsAtUtcMillis() > nowMillis) {
                upcoming.add(booking);
            } else {
                past.add(booking);
            }
        }
        Collections.sort(upcoming, (a, b) -> Long.compare(a.getStartsAtUtcMillis(), b.getStartsAtUtcMillis()));
        Collections.sort(past, (a, b) -> Long.compare(b.getStartsAtUtcMillis(), a.getStartsAtUtcMillis()));

        List<Row> rows = new ArrayList<>();
        if (!upcoming.isEmpty()) {
            rows.add(Row.header(R.string.my_bookings_upcoming));
            for (Booking booking : upcoming) {
                rows.add(Row.booking(booking));
            }
        }
        if (!past.isEmpty()) {
            rows.add(Row.header(R.string.my_bookings_past));
            for (Booking booking : past) {
                rows.add(Row.booking(booking));
            }
        }
        return Collections.unmodifiableList(rows);
    }
}
