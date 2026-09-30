package com.sportmate.app.presentation.booking;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.lifecycle.SavedStateHandle;

import com.sportmate.app.domain.common.AppError;
import com.sportmate.app.domain.model.BookingStatus;
import com.sportmate.app.domain.model.CourtSport;
import com.sportmate.app.domain.model.Sport;
import com.sportmate.app.presentation.common.Event;
import com.sportmate.app.presentation.common.UiState;
import com.sportmate.app.testutil.ControllableBookingRepository;
import com.sportmate.app.testutil.ControllableCourtDiscoveryRepository;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class BookingSummaryViewModelTest {
    private static final long START_UTC = 4_094_852_400_000L; // 2099-10-05 10:00 in Ho Chi Minh City

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    private final ControllableCourtDiscoveryRepository courts = new ControllableCourtDiscoveryRepository();
    private final ControllableBookingRepository bookings = new ControllableBookingRepository();
    private BookingSummaryViewModel viewModel;

    @Before
    public void setUp() {
        viewModel = viewModelFor("cs-2");
    }

    @Test
    public void summaryUsesTheChosenCourtSportAndCourtZone() {
        assertEquals(UiState.Status.LOADING, viewModel.getSummary().getValue().getStatus());

        loaded();

        BookingSummary summary = viewModel.getSummary().getValue().getData();
        assertEquals("Court A", summary.courtName);
        assertEquals("Asia/Ho_Chi_Minh", summary.timeZoneId);
        assertEquals("cs-2", summary.courtSportId);
        assertEquals("Tennis", summary.sportName);
        assertEquals(START_UTC, summary.startsAtUtcMillis);
        assertEquals(START_UTC + 90 * 60_000L, summary.endsAtUtcMillis);
        assertEquals(90, summary.minutes);
        assertEquals(new BigDecimal("300000.00"), summary.estimatedTotal);
    }

    @Test
    public void sportNoLongerOfferedIsNotFound() {
        viewModel = viewModelFor("cs-gone");
        loaded();

        assertEquals(AppError.Kind.NOT_FOUND, viewModel.getSummary().getValue().getError().getKind());
    }

    @Test
    public void confirmCreatesTheBookingOnceAndOpensIt() {
        loaded();

        viewModel.confirm();
        viewModel.confirm();

        assertEquals(1, bookings.createCalls);
        assertEquals("cs-2", bookings.lastCourtSportId);
        assertEquals(START_UTC, bookings.lastStart);
        assertEquals(START_UTC + 90 * 60_000L, bookings.lastEnd);
        assertTrue(viewModel.getSubmitting().getValue());

        bookings.createCallback.onSuccess(ControllableBookingRepository.booking(BookingStatus.PENDING));

        assertFalse(viewModel.getSubmitting().getValue());
        assertTrue(viewModel.isCreated());
        Event<String> created = viewModel.getCreated().getValue();
        assertEquals(ControllableBookingRepository.booking(BookingStatus.PENDING).getId(), created.getContentIfNotHandled());
        assertNull(created.getContentIfNotHandled());

        // Once created, it can never be submitted again.
        viewModel.confirm();
        assertEquals(1, bookings.createCalls);
    }

    @Test
    public void conflictIsReportedAndTheUserMayTryAgain() {
        loaded();
        viewModel.confirm();

        bookings.createCallback.onError(new AppError(AppError.Kind.CONFLICT, 409,
                "The court is already booked for part of this time.", null));

        assertFalse(viewModel.getSubmitting().getValue());
        assertEquals(AppError.Kind.CONFLICT, viewModel.getCreateError().getValue().getKind());
        assertNull(viewModel.getCreated().getValue());
        assertFalse(viewModel.isCreated());

        viewModel.confirm();
        assertEquals(2, bookings.createCalls);
        assertNull(viewModel.getCreateError().getValue());
    }

    @Test
    public void validationAndNetworkFailuresAreReported() {
        loaded();
        viewModel.confirm();
        bookings.createCallback.onError(new AppError(AppError.Kind.VALIDATION, 400, "One or more validation errors occurred.",
                Collections.singletonMap("starts_at", Collections.singletonList("Start time must be in the future."))));
        assertEquals(AppError.Kind.VALIDATION, viewModel.getCreateError().getValue().getKind());

        viewModel.confirm();
        bookings.createCallback.onError(AppError.of(AppError.Kind.NETWORK));
        assertEquals(AppError.Kind.NETWORK, viewModel.getCreateError().getValue().getKind());
    }

    @Test
    public void confirmIsIgnoredUntilTheSummaryLoads() {
        viewModel.confirm();

        assertEquals(0, bookings.createCalls);
    }

    @Test
    public void inFlightCreateIsNotCancelledWhenTheScreenGoes() {
        loaded();
        viewModel.confirm();
        int before = bookings.cancelled;

        viewModel.onCleared();

        assertEquals(before, bookings.cancelled);
    }

    private void loaded() {
        courts.courtCallback.onSuccess(ControllableCourtDiscoveryRepository.court("c-1", "Court A"));
        courts.sportsCallback.onSuccess(Arrays.asList(
                new CourtSport("cs-1", new Sport("s-1", "Badminton", "badminton"), new BigDecimal("150000"), "VND"),
                new CourtSport("cs-2", new Sport("s-2", "Tennis", "tennis"), new BigDecimal("200000"), "VND")));
    }

    private BookingSummaryViewModel viewModelFor(String courtSportId) {
        Map<String, Object> args = new HashMap<>();
        args.put(BookingSummaryViewModel.ARG_COURT_ID, "c-1");
        args.put(BookingSummaryViewModel.ARG_COURT_SPORT_ID, courtSportId);
        args.put(BookingSummaryViewModel.ARG_DATE, "2099-10-05");
        args.put(BookingSummaryViewModel.ARG_START_TIME, "10:00");
        args.put(BookingSummaryViewModel.ARG_END_TIME, "11:30");
        return new BookingSummaryViewModel(courts, bookings, new SavedStateHandle(args));
    }
}
