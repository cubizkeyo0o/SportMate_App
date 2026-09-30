package com.sportmate.app.presentation.booking;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.lifecycle.SavedStateHandle;

import com.sportmate.app.R;
import com.sportmate.app.domain.common.AppError;
import com.sportmate.app.domain.model.BookingStatus;
import com.sportmate.app.presentation.common.Event;
import com.sportmate.app.presentation.common.UiState;
import com.sportmate.app.presentation.common.UiText;
import com.sportmate.app.testutil.BookingJson;
import com.sportmate.app.testutil.ControllableBookingRepository;
import com.sportmate.app.testutil.ControllableCourtDiscoveryRepository;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.Collections;

public class BookingDetailViewModelTest {
    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    private final ControllableBookingRepository bookings = new ControllableBookingRepository();
    private final ControllableCourtDiscoveryRepository courts = new ControllableCourtDiscoveryRepository();
    private BookingDetailViewModel viewModel;

    @Before
    public void setUp() {
        viewModel = new BookingDetailViewModel(bookings, courts,
                new SavedStateHandle(Collections.singletonMap(BookingDetailViewModel.ARG_BOOKING_ID, BookingJson.BOOKING_ID)));
    }

    @Test
    public void loadsTheBookingThenItsCourtName() {
        assertEquals(BookingJson.BOOKING_ID, bookings.lastBookingId);
        assertEquals(UiState.Status.LOADING, status());

        bookings.getCallback.onSuccess(ControllableBookingRepository.booking(BookingStatus.PENDING));

        assertEquals(UiState.Status.SUCCESS, status());
        assertEquals(BookingJson.COURT_ID, courts.lastCourtId);
        assertNull(viewModel.getCourtName().getValue());
        courts.courtCallback.onSuccess(ControllableCourtDiscoveryRepository.court(BookingJson.COURT_ID, "Court A"));
        assertEquals(UiText.raw("Court A"), viewModel.getCourtName().getValue());
    }

    @Test
    public void unlistedCourtStillShowsTheBooking() {
        bookings.getCallback.onSuccess(ControllableBookingRepository.booking(BookingStatus.CONFIRMED));
        courts.courtCallback.onError(new AppError(AppError.Kind.NOT_FOUND, 404, "Court not found.", null));

        assertEquals(UiState.Status.SUCCESS, status());
        assertEquals(UiText.of(R.string.booking_court_unlisted), viewModel.getCourtName().getValue());
    }

    @Test
    public void loadFailureAndRetry() {
        bookings.getCallback.onError(new AppError(AppError.Kind.NOT_FOUND, 404, "Booking not found.", null));
        assertEquals(AppError.Kind.NOT_FOUND, viewModel.getBooking().getValue().getError().getKind());

        viewModel.retry();

        assertEquals(2, bookings.getCalls);
        assertEquals(UiState.Status.LOADING, status());
    }

    @Test
    public void paymentConfirmsTheBookingOnce() {
        bookings.getCallback.onSuccess(ControllableBookingRepository.booking(BookingStatus.PENDING));

        viewModel.pay();
        viewModel.pay();

        assertEquals(1, bookings.payCalls);
        assertTrue(viewModel.getBusy().getValue());

        bookings.payCallback.onSuccess(ControllableBookingRepository.booking(BookingStatus.CONFIRMED));

        assertFalse(viewModel.getBusy().getValue());
        assertEquals(BookingStatus.CONFIRMED, viewModel.getBooking().getValue().getData().getStatus());
        assertMessage(UiText.of(R.string.booking_payment_confirmed));
        viewModel.pay();
        assertEquals(1, bookings.payCalls);
    }

    @Test
    public void paymentFailureShowsTheBackendReasonAndReloads() {
        bookings.getCallback.onSuccess(ControllableBookingRepository.booking(BookingStatus.PENDING));
        viewModel.pay();

        bookings.payCallback.onError(new AppError(AppError.Kind.VALIDATION, 400, "One or more validation errors occurred.",
                Collections.singletonMap("status", Collections.singletonList(
                        "The hold on this booking has expired, so it was cancelled and can no longer be paid."))));

        assertFalse(viewModel.getBusy().getValue());
        assertMessage(UiText.raw("The hold on this booking has expired, so it was cancelled and can no longer be paid."));
        assertEquals(2, bookings.getCalls);
        // The reload keeps the content on screen and applies the backend's new status.
        assertEquals(UiState.Status.SUCCESS, status());
        bookings.getCallback.onSuccess(ControllableBookingRepository.booking(BookingStatus.CANCELLED));
        assertEquals(BookingStatus.CANCELLED, viewModel.getBooking().getValue().getData().getStatus());
    }

    @Test
    public void payOnlyForPendingAndCancelOnlyForPendingOrConfirmed() {
        bookings.getCallback.onSuccess(ControllableBookingRepository.booking(BookingStatus.CANCELLED));

        viewModel.pay();
        viewModel.cancel();

        assertEquals(0, bookings.payCalls);
        assertEquals(0, bookings.cancelCalls);
        assertTrue(BookingDetailViewModel.canCancel(ControllableBookingRepository.booking(BookingStatus.CONFIRMED)));
        assertFalse(BookingDetailViewModel.canPay(ControllableBookingRepository.booking(BookingStatus.CONFIRMED)));
        assertFalse(BookingDetailViewModel.canCancel(ControllableBookingRepository.booking(BookingStatus.COMPLETED)));
    }

    @Test
    public void cancellationAppliesTheBackendResult() {
        bookings.getCallback.onSuccess(ControllableBookingRepository.booking(BookingStatus.CONFIRMED));

        viewModel.cancel();
        viewModel.cancel();

        assertEquals(1, bookings.cancelCalls);
        bookings.cancelCallback.onSuccess(ControllableBookingRepository.booking(BookingStatus.CANCELLED));

        assertEquals(BookingStatus.CANCELLED, viewModel.getBooking().getValue().getData().getStatus());
        assertMessage(UiText.of(R.string.booking_cancelled_message));
    }

    @Test
    public void cancellationFailureShowsTheReasonAndReloads() {
        bookings.getCallback.onSuccess(ControllableBookingRepository.booking(BookingStatus.CONFIRMED));
        viewModel.cancel();

        bookings.cancelCallback.onError(new AppError(AppError.Kind.VALIDATION, 400, "One or more validation errors occurred.",
                Collections.singletonMap("status", Collections.singletonList("A booking cannot be cancelled once it has started."))));

        assertMessage(UiText.raw("A booking cannot be cancelled once it has started."));
        assertEquals(2, bookings.getCalls);
    }

    @Test
    public void refreshReloadsOnlyLoadedIdleContent() {
        viewModel.refresh();
        assertEquals(1, bookings.getCalls);

        bookings.getCallback.onSuccess(ControllableBookingRepository.booking(BookingStatus.PENDING));
        viewModel.refresh();
        assertEquals(2, bookings.getCalls);
        assertEquals(UiState.Status.SUCCESS, status());
    }

    private UiState.Status status() {
        return viewModel.getBooking().getValue().getStatus();
    }

    private void assertMessage(UiText expected) {
        Event<UiText> event = viewModel.getMessage().getValue();
        assertEquals(expected, event.getContentIfNotHandled());
        assertNull(event.getContentIfNotHandled());
    }
}
