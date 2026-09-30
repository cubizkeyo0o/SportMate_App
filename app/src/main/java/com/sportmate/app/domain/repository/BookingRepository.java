package com.sportmate.app.domain.repository;

import com.sportmate.app.domain.common.Cancellable;
import com.sportmate.app.domain.common.ResultCallback;
import com.sportmate.app.domain.model.Booking;

import java.util.List;

/**
 * The signed-in user's bookings. The backend decides availability, price and every status change;
 * a booking the caller may not see is reported as NOT_FOUND.
 */
public interface BookingRepository {
    /**
     * Creates a Pending booking for a court-sport link (from court discovery) between two instants.
     * CONFLICT means the court is already booked for part of that time.
     */
    Cancellable createBooking(
            String courtSportId,
            long startsAtUtcMillis,
            long endsAtUtcMillis,
            ResultCallback<Booking> callback);

    Cancellable getBooking(String bookingId, ResultCallback<Booking> callback);

    /** Ordered by start time. */
    Cancellable getMyBookings(ResultCallback<List<Booking>> callback);

    /** Mock payment: a Pending booking becomes Confirmed if its hold is still running. */
    Cancellable pay(String bookingId, ResultCallback<Booking> callback);

    /** Cancels a Pending or Confirmed booking before it starts. */
    Cancellable cancel(String bookingId, ResultCallback<Booking> callback);
}
