package com.sportmate.app.data.repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.sportmate.app.data.remote.api.BookingApi;
import com.sportmate.app.domain.common.AppError;
import com.sportmate.app.domain.model.Booking;
import com.sportmate.app.domain.model.BookingStatus;
import com.sportmate.app.testutil.BlockingCallback;
import com.sportmate.app.testutil.BookingJson;
import com.sportmate.app.testutil.FakeTokenStorage;
import com.sportmate.app.testutil.TestNetwork;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

/** api/v1/bookings against the backend's JSON shapes and status codes. */
public class BookingRepositoryImplTest {
    // 2099-10-05T03:00:00Z and 04:30Z.
    private static final long START = 4_094_852_400_000L;
    private static final long END = START + 90 * 60_000L;

    private MockWebServer server;
    private BookingRepositoryImpl repository;

    @Before
    public void setUp() throws Exception {
        server = new MockWebServer();
        server.start();
        BookingApi api = TestNetwork.retrofit(server, new FakeTokenStorage("access-1", "refresh-1")).create(BookingApi.class);
        repository = new BookingRepositoryImpl(api, TestNetwork.executor());
    }

    @After
    public void tearDown() throws Exception {
        server.shutdown();
    }

    // --- Create ---

    @Test
    public void createSendsCourtSportIdAndUtcInstantsAndMapsPendingBooking() throws Exception {
        server.enqueue(json(201, BookingJson.pending()));

        Booking booking = create().result();

        RecordedRequest request = server.takeRequest();
        assertEquals("POST", request.getMethod());
        assertEquals("/api/v1/bookings", request.getPath());
        assertEquals("Bearer access-1", request.getHeader("Authorization"));
        assertEquals("{\"court_sport_id\":\"" + BookingJson.COURT_SPORT_ID + "\","
                + "\"starts_at\":\"2099-10-05T03:00:00Z\",\"ends_at\":\"2099-10-05T04:30:00Z\"}",
                request.getBody().readUtf8());

        assertEquals(BookingJson.BOOKING_ID, booking.getId());
        assertEquals(BookingJson.COURT_ID, booking.getCourtId());
        assertEquals(BookingJson.COURT_SPORT_ID, booking.getCourtSportId());
        assertEquals("Badminton", booking.getSport().getName());
        assertEquals(START, booking.getStartsAtUtcMillis());
        assertEquals(END, booking.getEndsAtUtcMillis());
        assertEquals("Asia/Ho_Chi_Minh", booking.getTimeZoneId());
        assertEquals("2099-10-05", booking.getLocalDate());
        assertEquals("10:00", booking.getLocalStartTime());
        assertEquals("11:30", booking.getLocalEndTime());
        assertEquals(BookingStatus.PENDING, booking.getStatus());
        assertEquals(new BigDecimal("225000.00"), booking.getTotalPrice());
        assertEquals("VND", booking.getCurrency());
        // Fractional seconds are read to the millisecond.
        assertEquals(Long.valueOf(START - (4 * 24 + 1) * 3_600_000L + 15 * 60_000L + 123L), booking.getHoldExpiresAtUtcMillis());
        assertEquals(START - (4 * 24 + 1) * 3_600_000L + 123L, booking.getCreatedAtUtcMillis());
        assertNull(booking.getCancellationReason());
        assertNull(booking.getCancelledAtUtcMillis());
    }

    @Test
    public void createFailures() throws Exception {
        server.enqueue(problem(400, "{\"title\":\"One or more validation errors occurred.\",\"status\":400,"
                + "\"errors\":{\"starts_at\":[\"The court has no operating hours on Sunday.\"]}}"));
        server.enqueue(new MockResponse().setResponseCode(401).addHeader("WWW-Authenticate", "Bearer"));
        server.enqueue(problem(404, "{\"title\":\"Court sport not found.\",\"status\":404}"));
        server.enqueue(problem(409, "{\"title\":\"The court is already booked for part of this time.\",\"status\":409}"));
        server.enqueue(new MockResponse().setResponseCode(500));

        AppError validation = create().error();
        assertEquals(AppError.Kind.VALIDATION, validation.getKind());
        assertEquals(Collections.singletonList("The court has no operating hours on Sunday."),
                validation.getFieldErrors().get("starts_at"));
        assertEquals(AppError.Kind.UNAUTHORIZED, create().error().getKind());
        assertEquals(AppError.Kind.NOT_FOUND, create().error().getKind());
        AppError conflict = create().error();
        assertEquals(AppError.Kind.CONFLICT, conflict.getKind());
        assertEquals("The court is already booked for part of this time.", conflict.getMessage());
        assertEquals(AppError.Kind.SERVER, create().error().getKind());

        server.shutdown();
        assertEquals(AppError.Kind.NETWORK, create().error().getKind());
    }

    // --- Read ---

    @Test
    public void getLoadsBookingById() throws Exception {
        server.enqueue(json(200, BookingJson.cancelled()));

        BlockingCallback<Booking> callback = new BlockingCallback<>();
        repository.getBooking(BookingJson.BOOKING_ID, callback);
        Booking booking = callback.await().result();

        RecordedRequest request = server.takeRequest();
        assertEquals("GET", request.getMethod());
        assertEquals("/api/v1/bookings/" + BookingJson.BOOKING_ID, request.getPath());
        assertEquals(BookingStatus.CANCELLED, booking.getStatus());
        assertNull(booking.getHoldExpiresAtUtcMillis());
        assertEquals(Long.valueOf(START - (4 * 24) * 3_600_000L), booking.getCancelledAtUtcMillis());
    }

    @Test
    public void getOfUnknownOrHiddenBookingIsNotFound() throws Exception {
        server.enqueue(problem(404, "{\"title\":\"Booking not found.\",\"status\":404}"));

        BlockingCallback<Booking> callback = new BlockingCallback<>();
        repository.getBooking(BookingJson.BOOKING_ID, callback);

        assertEquals(AppError.Kind.NOT_FOUND, callback.await().error().getKind());
    }

    @Test
    public void myBookingsMapsEveryStatus() throws Exception {
        server.enqueue(json(200, "[" + BookingJson.booking("pending", "null", "null", "null") + ","
                + BookingJson.booking("confirmed", "null", "null", "null") + ","
                + BookingJson.booking("cancelled", "null", "\"Hold expired\"", "\"2099-10-01T02:15:00Z\"") + ","
                + BookingJson.booking("completed", "null", "null", "null") + ","
                + BookingJson.booking("no_show", "null", "null", "null") + ","
                + BookingJson.booking("something_new", "null", "null", "null") + "]"));

        BlockingCallback<List<Booking>> callback = new BlockingCallback<>();
        repository.getMyBookings(callback);
        List<Booking> bookings = callback.await().result();

        RecordedRequest request = server.takeRequest();
        assertEquals("GET", request.getMethod());
        assertEquals("/api/v1/bookings/me", request.getPath());
        assertEquals(BookingStatus.PENDING, bookings.get(0).getStatus());
        assertEquals(BookingStatus.CONFIRMED, bookings.get(1).getStatus());
        assertEquals(BookingStatus.CANCELLED, bookings.get(2).getStatus());
        assertEquals("Hold expired", bookings.get(2).getCancellationReason());
        assertEquals(BookingStatus.COMPLETED, bookings.get(3).getStatus());
        assertEquals(BookingStatus.NO_SHOW, bookings.get(4).getStatus());
        assertEquals(BookingStatus.UNKNOWN, bookings.get(5).getStatus());
    }

    @Test
    public void emptyMyBookingsIsSuccess() throws Exception {
        server.enqueue(json(200, "[]"));

        BlockingCallback<List<Booking>> callback = new BlockingCallback<>();
        repository.getMyBookings(callback);

        assertTrue(callback.await().result().isEmpty());
    }

    @Test
    public void malformedBookingIsAnUnreadableResponse() throws Exception {
        server.enqueue(json(200, BookingJson.pending().replace("\"local_start_time\":\"10:00\"", "\"local_start_time\":\"10am\"")));
        server.enqueue(json(200, BookingJson.pending().replace("\"2099-10-05T03:00:00Z\"", "\"05/10/2099\"")));

        BlockingCallback<Booking> first = new BlockingCallback<>();
        repository.getBooking(BookingJson.BOOKING_ID, first);
        BlockingCallback<Booking> second = new BlockingCallback<>();
        repository.getBooking(BookingJson.BOOKING_ID, second);

        assertEquals(AppError.Kind.UNKNOWN, first.await().error().getKind());
        assertEquals(AppError.Kind.UNKNOWN, second.await().error().getKind());
    }

    // --- Pay and cancel ---

    @Test
    public void payPostsWithoutBodyAndReturnsConfirmedBooking() throws Exception {
        server.enqueue(json(200, BookingJson.confirmed()));

        BlockingCallback<Booking> callback = new BlockingCallback<>();
        repository.pay(BookingJson.BOOKING_ID, callback);
        Booking booking = callback.await().result();

        RecordedRequest request = server.takeRequest();
        assertEquals("POST", request.getMethod());
        assertEquals("/api/v1/bookings/" + BookingJson.BOOKING_ID + "/pay", request.getPath());
        assertEquals("Bearer access-1", request.getHeader("Authorization"));
        assertEquals(0, request.getBodySize());
        assertEquals(BookingStatus.CONFIRMED, booking.getStatus());
    }

    @Test
    public void payRuleViolationsAreStatusValidationErrors() throws Exception {
        server.enqueue(problem(400, "{\"title\":\"One or more validation errors occurred.\",\"status\":400,"
                + "\"errors\":{\"status\":[\"The hold on this booking has expired, so it was cancelled and can no longer be paid.\"]}}"));
        server.enqueue(problem(403, "{\"title\":\"Only the person who made the booking can pay for it.\",\"status\":403}"));

        BlockingCallback<Booking> expired = new BlockingCallback<>();
        repository.pay(BookingJson.BOOKING_ID, expired);
        AppError error = expired.await().error();
        assertEquals(AppError.Kind.VALIDATION, error.getKind());
        assertTrue(error.getFieldErrors().get("status").get(0).startsWith("The hold on this booking has expired"));

        BlockingCallback<Booking> forbidden = new BlockingCallback<>();
        repository.pay(BookingJson.BOOKING_ID, forbidden);
        assertEquals(AppError.Kind.FORBIDDEN, forbidden.await().error().getKind());
    }

    @Test
    public void cancelPostsWithoutBodyAndReturnsCancelledBooking() throws Exception {
        server.enqueue(json(200, BookingJson.cancelled()));
        server.enqueue(problem(400, "{\"title\":\"One or more validation errors occurred.\",\"status\":400,"
                + "\"errors\":{\"status\":[\"A booking cannot be cancelled once it has started.\"]}}"));

        BlockingCallback<Booking> callback = new BlockingCallback<>();
        repository.cancel(BookingJson.BOOKING_ID, callback);
        assertEquals(BookingStatus.CANCELLED, callback.await().result().getStatus());

        RecordedRequest request = server.takeRequest();
        assertEquals("POST", request.getMethod());
        assertEquals("/api/v1/bookings/" + BookingJson.BOOKING_ID + "/cancel", request.getPath());
        assertEquals(0, request.getBodySize());

        BlockingCallback<Booking> started = new BlockingCallback<>();
        repository.cancel(BookingJson.BOOKING_ID, started);
        assertEquals(AppError.Kind.VALIDATION, started.await().error().getKind());
    }

    private BlockingCallback<Booking> create() throws InterruptedException {
        BlockingCallback<Booking> callback = new BlockingCallback<>();
        repository.createBooking(BookingJson.COURT_SPORT_ID, START, END, callback);
        return callback.await();
    }

    private static MockResponse json(int code, String body) {
        return new MockResponse().setResponseCode(code).addHeader("Content-Type", "application/json").setBody(body);
    }

    private static MockResponse problem(int code, String body) {
        return new MockResponse()
                .setResponseCode(code)
                .addHeader("Content-Type", "application/problem+json")
                .setBody(body);
    }
}
