package com.sportmate.app.presentation.booking;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.lifecycle.SavedStateHandle;

import com.sportmate.app.data.remote.api.BookingApi;
import com.sportmate.app.data.remote.api.CourtDiscoveryApi;
import com.sportmate.app.data.repository.BookingRepositoryImpl;
import com.sportmate.app.data.repository.CourtDiscoveryRepositoryImpl;
import com.sportmate.app.domain.common.AppError;
import com.sportmate.app.domain.model.Booking;
import com.sportmate.app.domain.model.BookingStatus;
import com.sportmate.app.presentation.common.UiState;
import com.sportmate.app.testutil.BookingJson;
import com.sportmate.app.testutil.FakeTokenStorage;
import com.sportmate.app.testutil.LiveDataTestUtil;
import com.sportmate.app.testutil.TestNetwork;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import retrofit2.Retrofit;

/**
 * Summary, create, pay and cancel through the real repositories, Retrofit and OkHttp, against a
 * stub that answers like the backend for one court.
 */
public class BookingFlowIntegrationTest {
    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    private MockWebServer server;
    private BackendStub backend;
    private CourtDiscoveryRepositoryImpl courts;
    private BookingRepositoryImpl bookings;

    @Before
    public void setUp() throws Exception {
        server = new MockWebServer();
        backend = new BackendStub();
        server.setDispatcher(backend);
        server.start();
        Retrofit retrofit = TestNetwork.retrofit(server, new FakeTokenStorage("access-1", "refresh-1"));
        courts = new CourtDiscoveryRepositoryImpl(retrofit.create(CourtDiscoveryApi.class), TestNetwork.executor());
        bookings = new BookingRepositoryImpl(retrofit.create(BookingApi.class), TestNetwork.executor());
    }

    @After
    public void tearDown() throws Exception {
        server.shutdown();
    }

    @Test
    public void createPayAndCancel() throws Exception {
        BookingSummaryViewModel summary = summary();
        BookingSummary data = LiveDataTestUtil.await(summary.getSummary(), s -> s.getStatus() == UiState.Status.SUCCESS).getData();
        assertEquals(new BigDecimal("225000.00"), data.estimatedTotal);

        summary.confirm();
        String bookingId = LiveDataTestUtil.await(summary.getCreated(), e -> true).getContentIfNotHandled();
        assertEquals(BookingJson.BOOKING_ID, bookingId);
        assertEquals("{\"court_sport_id\":\"" + BookingJson.COURT_SPORT_ID + "\","
                + "\"starts_at\":\"2099-10-05T03:00:00Z\",\"ends_at\":\"2099-10-05T04:30:00Z\"}", backend.createBody);

        BookingDetailViewModel detail = new BookingDetailViewModel(bookings, courts,
                new SavedStateHandle(Collections.singletonMap(BookingDetailViewModel.ARG_BOOKING_ID, bookingId)));
        Booking pending = LiveDataTestUtil.await(detail.getBooking(), s -> s.getStatus() == UiState.Status.SUCCESS).getData();
        assertEquals(BookingStatus.PENDING, pending.getStatus());
        // The backend's price snapshot, not the estimate, is what the booking shows.
        assertEquals(new BigDecimal("225000.00"), pending.getTotalPrice());
        LiveDataTestUtil.await(detail.getCourtName(), name -> true);

        detail.pay();
        LiveDataTestUtil.await(detail.getMessage(), e -> true);
        assertEquals(BookingStatus.CONFIRMED, detail.getBooking().getValue().getData().getStatus());

        detail.cancel();
        LiveDataTestUtil.await(detail.getBooking(),
                s -> s.getData() != null && s.getData().getStatus() == BookingStatus.CANCELLED);
        assertFalse(detail.getBusy().getValue());
        assertEquals(1, backend.creates.get());
        assertEquals(1, backend.payments.get());
    }

    @Test
    public void conflictLeavesNoBookingAndCanBeRetried() throws Exception {
        backend.conflict = true;
        BookingSummaryViewModel summary = summary();
        LiveDataTestUtil.await(summary.getSummary(), s -> s.getStatus() == UiState.Status.SUCCESS);

        summary.confirm();
        AppError error = LiveDataTestUtil.await(summary.getCreateError(), e -> true);

        assertEquals(AppError.Kind.CONFLICT, error.getKind());
        assertFalse(summary.isCreated());

        backend.conflict = false;
        summary.confirm();
        LiveDataTestUtil.await(summary.getCreated(), e -> true);
        assertEquals(2, backend.creates.get());
    }

    private BookingSummaryViewModel summary() {
        Map<String, Object> args = new HashMap<>();
        args.put(BookingSummaryViewModel.ARG_COURT_ID, BookingJson.COURT_ID);
        args.put(BookingSummaryViewModel.ARG_COURT_SPORT_ID, BookingJson.COURT_SPORT_ID);
        args.put(BookingSummaryViewModel.ARG_DATE, "2099-10-05");
        args.put(BookingSummaryViewModel.ARG_START_TIME, "10:00");
        args.put(BookingSummaryViewModel.ARG_END_TIME, "11:30");
        return new BookingSummaryViewModel(courts, bookings, new SavedStateHandle(args));
    }

    /** Answers like the backend for one active court and one booking. */
    private static final class BackendStub extends Dispatcher {
        final AtomicInteger creates = new AtomicInteger();
        final AtomicInteger payments = new AtomicInteger();
        volatile boolean conflict;
        volatile String createBody;
        private volatile String status = "pending";

        @Override
        public MockResponse dispatch(RecordedRequest request) {
            if (!"Bearer access-1".equals(request.getHeader("Authorization"))) {
                return new MockResponse().setResponseCode(401);
            }
            String route = request.getMethod() + " " + request.getPath();
            String court = "/api/v1/discovery/courts/" + BookingJson.COURT_ID;
            String booking = "/api/v1/bookings/" + BookingJson.BOOKING_ID;
            if (route.equals("GET " + court)) {
                return json(200, "{\"id\":\"" + BookingJson.COURT_ID + "\",\"name\":\"Court A\",\"description\":null,"
                        + "\"address\":\"12 Tran Hung Dao\",\"city\":\"Hanoi\",\"latitude\":21.028511,"
                        + "\"longitude\":105.804817,\"time_zone_id\":\"Asia/Ho_Chi_Minh\",\"status\":\"active\"}");
            }
            if (route.equals("GET " + court + "/sports")) {
                return json(200, "[{\"court_sport_id\":\"" + BookingJson.COURT_SPORT_ID + "\","
                        + "\"sport\":{\"id\":\"s-1\",\"name\":\"Badminton\",\"slug\":\"badminton\"},"
                        + "\"price_per_hour\":150000.00,\"currency\":\"VND\",\"is_active\":true}]");
            }
            if (route.equals("POST /api/v1/bookings")) {
                creates.incrementAndGet();
                createBody = request.getBody().readUtf8();
                if (conflict) {
                    return json(409, "{\"title\":\"The court is already booked for part of this time.\",\"status\":409}");
                }
                status = "pending";
                return json(201, BookingJson.pending());
            }
            if (route.equals("GET " + booking)) {
                return json(200, current());
            }
            if (route.equals("POST " + booking + "/pay")) {
                payments.incrementAndGet();
                status = "confirmed";
                return json(200, current());
            }
            if (route.equals("POST " + booking + "/cancel")) {
                status = "cancelled";
                return json(200, current());
            }
            return json(404, "{\"title\":\"Not found.\",\"status\":404}");
        }

        private String current() {
            switch (status) {
                case "confirmed":
                    return BookingJson.confirmed();
                case "cancelled":
                    return BookingJson.cancelled();
                default:
                    return BookingJson.pending();
            }
        }

        private static MockResponse json(int code, String body) {
            return new MockResponse().setResponseCode(code).addHeader("Content-Type", "application/json").setBody(body);
        }
    }
}
