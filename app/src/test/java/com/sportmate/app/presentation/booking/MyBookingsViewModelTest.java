package com.sportmate.app.presentation.booking;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import com.sportmate.app.domain.common.AppError;
import com.sportmate.app.domain.model.Booking;
import com.sportmate.app.domain.model.BookingStatus;
import com.sportmate.app.presentation.common.UiState;
import com.sportmate.app.testutil.BookingJson;
import com.sportmate.app.testutil.ControllableBookingRepository;
import com.sportmate.app.testutil.ControllableCourtDiscoveryRepository;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.Collections;
import java.util.List;

public class MyBookingsViewModelTest {
    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    private final ControllableBookingRepository bookings = new ControllableBookingRepository();
    private final ControllableCourtDiscoveryRepository courts = new ControllableCourtDiscoveryRepository();
    private MyBookingsViewModel viewModel;

    @Before
    public void setUp() {
        viewModel = new MyBookingsViewModel(bookings, courts);
    }

    @Test
    public void loadingThenContentWithCourtNames() {
        assertEquals(UiState.Status.LOADING, status());
        List<Booking> list = Collections.singletonList(ControllableBookingRepository.booking(BookingStatus.CONFIRMED));

        bookings.listCallback.onSuccess(list);
        courts.courtsCallback.onSuccess(Collections.singletonList(
                ControllableCourtDiscoveryRepository.court(BookingJson.COURT_ID, "Court A")));

        assertEquals(UiState.Status.SUCCESS, status());
        assertEquals(list, viewModel.getBookings().getValue().getData());
        assertEquals("Court A", viewModel.getCourtNames().getValue().get(BookingJson.COURT_ID));
    }

    @Test
    public void emptyList() {
        bookings.listCallback.onSuccess(Collections.emptyList());

        assertEquals(UiState.Status.SUCCESS, status());
        assertTrue(viewModel.getBookings().getValue().getData().isEmpty());
    }

    @Test
    public void courtNameFailureDoesNotFailTheList() {
        courts.courtsCallback.onError(AppError.of(AppError.Kind.NETWORK));
        bookings.listCallback.onSuccess(Collections.singletonList(ControllableBookingRepository.booking(BookingStatus.PENDING)));

        assertEquals(UiState.Status.SUCCESS, status());
        assertTrue(viewModel.getCourtNames().getValue().isEmpty());
    }

    @Test
    public void errorThenRetry() {
        bookings.listCallback.onError(new AppError(AppError.Kind.UNAUTHORIZED, 401, null, null));
        assertEquals(UiState.Status.ERROR, status());

        viewModel.retry();

        assertEquals(UiState.Status.LOADING, status());
        assertEquals(2, bookings.listCalls);
    }

    @Test
    public void refreshKeepsContentWhileReloading() {
        viewModel.refresh();
        assertEquals(1, bookings.listCalls);

        bookings.listCallback.onSuccess(Collections.emptyList());
        viewModel.refresh();

        assertEquals(2, bookings.listCalls);
        assertEquals(UiState.Status.SUCCESS, status());
    }

    private UiState.Status status() {
        return viewModel.getBookings().getValue().getStatus();
    }
}
