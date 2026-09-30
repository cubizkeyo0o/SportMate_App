package com.sportmate.app.testutil;

import com.sportmate.app.domain.common.Cancellable;
import com.sportmate.app.domain.common.ResultCallback;
import com.sportmate.app.domain.model.Booking;
import com.sportmate.app.domain.model.BookingStatus;
import com.sportmate.app.domain.model.Sport;
import com.sportmate.app.domain.repository.BookingRepository;

import java.math.BigDecimal;
import java.util.List;

/**
 * Records calls and keeps each callback so a test can complete it. Used for ViewModel state tests;
 * HTTP behaviour is covered against MockWebServer.
 */
public final class ControllableBookingRepository implements BookingRepository {
    public int createCalls;
    public int getCalls;
    public int listCalls;
    public int payCalls;
    public int cancelCalls;
    public int cancelled;
    public String lastCourtSportId;
    public long lastStart;
    public long lastEnd;
    public String lastBookingId;

    public ResultCallback<Booking> createCallback;
    public ResultCallback<Booking> getCallback;
    public ResultCallback<List<Booking>> listCallback;
    public ResultCallback<Booking> payCallback;
    public ResultCallback<Booking> cancelCallback;

    public static Booking booking(String id, BookingStatus status, long start, long end) {
        return new Booking(id, BookingJson.COURT_ID, BookingJson.COURT_SPORT_ID, new Sport("s-1", "Badminton", "badminton"),
                start, end, "Asia/Ho_Chi_Minh", "2099-10-05", "10:00", "11:30", status, new BigDecimal("225000.00"), "VND",
                status == BookingStatus.PENDING ? start - 3_600_000L : null, null, null, start - 7_200_000L);
    }

    public static Booking booking(BookingStatus status) {
        return booking(BookingJson.BOOKING_ID, status, 4_094_852_400_000L, 4_094_852_400_000L + 90 * 60_000L);
    }

    @Override
    public Cancellable createBooking(String courtSportId, long startsAtUtcMillis, long endsAtUtcMillis,
            ResultCallback<Booking> callback) {
        createCalls++;
        lastCourtSportId = courtSportId;
        lastStart = startsAtUtcMillis;
        lastEnd = endsAtUtcMillis;
        createCallback = callback;
        return () -> cancelled++;
    }

    @Override
    public Cancellable getBooking(String bookingId, ResultCallback<Booking> callback) {
        getCalls++;
        lastBookingId = bookingId;
        getCallback = callback;
        return () -> cancelled++;
    }

    @Override
    public Cancellable getMyBookings(ResultCallback<List<Booking>> callback) {
        listCalls++;
        listCallback = callback;
        return () -> cancelled++;
    }

    @Override
    public Cancellable pay(String bookingId, ResultCallback<Booking> callback) {
        payCalls++;
        lastBookingId = bookingId;
        payCallback = callback;
        return () -> cancelled++;
    }

    @Override
    public Cancellable cancel(String bookingId, ResultCallback<Booking> callback) {
        cancelCalls++;
        lastBookingId = bookingId;
        cancelCallback = callback;
        return () -> cancelled++;
    }
}
