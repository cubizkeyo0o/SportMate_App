package com.sportmate.app.presentation.booking;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;

import com.sportmate.app.R;
import com.sportmate.app.domain.common.AppError;
import com.sportmate.app.domain.common.Cancellable;
import com.sportmate.app.domain.common.ResultCallback;
import com.sportmate.app.domain.model.Booking;
import com.sportmate.app.domain.model.BookingStatus;
import com.sportmate.app.domain.model.Court;
import com.sportmate.app.domain.repository.BookingRepository;
import com.sportmate.app.domain.repository.CourtDiscoveryRepository;
import com.sportmate.app.presentation.common.Event;
import com.sportmate.app.presentation.common.UiState;
import com.sportmate.app.presentation.common.UiText;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

/**
 * One booking, with mock payment and cancellation. Every status shown comes from the backend; after
 * a failed payment or cancellation the booking is reloaded, because the backend may have changed it
 * (for example, cancelled an expired hold).
 */
@HiltViewModel
public final class BookingDetailViewModel extends ViewModel {
    /** Navigation argument: the booking to show. */
    public static final String ARG_BOOKING_ID = "bookingId";

    private final BookingRepository bookings;
    private final CourtDiscoveryRepository courts;
    private final String bookingId;
    private final MutableLiveData<UiState<Booking>> booking = new MutableLiveData<>();
    private final MutableLiveData<UiText> courtName = new MutableLiveData<>();
    private final MutableLiveData<Boolean> busy = new MutableLiveData<>(false);
    private final MutableLiveData<Event<UiText>> message = new MutableLiveData<>();
    private Cancellable pendingLoad;
    private Cancellable pendingCourt;
    private String loadedCourtId;

    @Inject
    public BookingDetailViewModel(
            BookingRepository bookings,
            CourtDiscoveryRepository courts,
            SavedStateHandle savedStateHandle) {
        this.bookings = bookings;
        this.courts = courts;
        this.bookingId = savedStateHandle.get(ARG_BOOKING_ID);
        load();
    }

    public LiveData<UiState<Booking>> getBooking() {
        return booking;
    }

    /** The court's name, a "no longer listed" text if it cannot be found, or null while loading. */
    public LiveData<UiText> getCourtName() {
        return courtName;
    }

    /** Whether a payment or cancellation is in flight. */
    public LiveData<Boolean> getBusy() {
        return busy;
    }

    public LiveData<Event<UiText>> getMessage() {
        return message;
    }

    public void retry() {
        UiState<Booking> current = booking.getValue();
        if (current != null && current.getStatus() == UiState.Status.ERROR) {
            load();
        }
    }

    /** Reloads without leaving the content, e.g. when the screen is shown again. */
    public void refresh() {
        UiState<Booking> current = booking.getValue();
        if (current != null && current.getStatus() == UiState.Status.SUCCESS && !isBusy()) {
            fetch(false);
        }
    }

    public static boolean canPay(@Nullable Booking booking) {
        return booking != null && booking.getStatus() == BookingStatus.PENDING;
    }

    /** Cancellation is offered for Pending and Confirmed bookings; the backend checks the time rule. */
    public static boolean canCancel(@Nullable Booking booking) {
        return booking != null
                && (booking.getStatus() == BookingStatus.PENDING || booking.getStatus() == BookingStatus.CONFIRMED);
    }

    /** Mock payment. Ignored unless Pending and nothing else is in flight. */
    public void pay() {
        if (isBusy() || !canPay(currentBooking())) {
            return;
        }
        busy.setValue(true);
        bookings.pay(bookingId, new ChangeCallback(R.string.booking_payment_confirmed));
    }

    public void cancel() {
        if (isBusy() || !canCancel(currentBooking())) {
            return;
        }
        busy.setValue(true);
        bookings.cancel(bookingId, new ChangeCallback(R.string.booking_cancelled_message));
    }

    @Override
    protected void onCleared() {
        cancel(pendingLoad);
        cancel(pendingCourt);
    }

    private void load() {
        if (bookingId == null) {
            booking.setValue(UiState.error(AppError.of(AppError.Kind.NOT_FOUND)));
            return;
        }
        fetch(true);
    }

    private void fetch(boolean showLoading) {
        cancel(pendingLoad);
        if (showLoading) {
            booking.setValue(UiState.loading());
        }
        pendingLoad = bookings.getBooking(bookingId, new ResultCallback<Booking>() {
            @Override
            public void onSuccess(Booking result) {
                show(result);
            }

            @Override
            public void onError(@NonNull AppError error) {
                if (showLoading) {
                    booking.setValue(UiState.error(error));
                }
            }
        });
    }

    private void show(Booking result) {
        booking.setValue(UiState.success(result));
        if (!result.getCourtId().equals(loadedCourtId)) {
            loadedCourtId = result.getCourtId();
            loadCourtName(result.getCourtId());
        }
    }

    // BookingResponse carries only the court id; the name comes from court discovery, which lists
    // active courts only.
    private void loadCourtName(String courtId) {
        courtName.setValue(null);
        pendingCourt = courts.getCourt(courtId, new ResultCallback<Court>() {
            @Override
            public void onSuccess(Court court) {
                courtName.setValue(UiText.raw(court.getName()));
            }

            @Override
            public void onError(@NonNull AppError error) {
                courtName.setValue(UiText.of(R.string.booking_court_unlisted));
            }
        });
    }

    private boolean isBusy() {
        return Boolean.TRUE.equals(busy.getValue());
    }

    @Nullable
    private Booking currentBooking() {
        UiState<Booking> current = booking.getValue();
        return current == null ? null : current.getData();
    }

    private static void cancel(Cancellable cancellable) {
        if (cancellable != null) {
            cancellable.cancel();
        }
    }

    /** Applies the backend's updated booking, or reports the failure and reloads. */
    private final class ChangeCallback implements ResultCallback<Booking> {
        private final int successMessage;

        ChangeCallback(int successMessage) {
            this.successMessage = successMessage;
        }

        @Override
        public void onSuccess(Booking result) {
            busy.setValue(false);
            show(result);
            message.setValue(new Event<>(UiText.of(successMessage)));
        }

        @Override
        public void onError(@NonNull AppError error) {
            busy.setValue(false);
            message.setValue(new Event<>(BookingMessages.forBooking(error)));
            fetch(false);
        }
    }
}
