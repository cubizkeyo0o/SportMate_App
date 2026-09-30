package com.sportmate.app.presentation.booking;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;

import com.sportmate.app.domain.common.AppError;
import com.sportmate.app.domain.common.Cancellable;
import com.sportmate.app.domain.common.ResultCallback;
import com.sportmate.app.domain.model.Booking;
import com.sportmate.app.domain.model.Court;
import com.sportmate.app.domain.model.CourtSport;
import com.sportmate.app.domain.repository.BookingRepository;
import com.sportmate.app.domain.repository.CourtDiscoveryRepository;
import com.sportmate.app.presentation.common.Event;
import com.sportmate.app.presentation.common.UiState;

import java.util.List;
import java.util.TimeZone;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

/**
 * Reviews the chosen booking and creates it. Court, sport and price are reloaded from court
 * discovery rather than passed along, so the summary reflects the backend's current data. A booking
 * is created at most once per summary, whatever the user taps or however the screen is recreated.
 */
@HiltViewModel
public final class BookingSummaryViewModel extends ViewModel {
    public static final String ARG_COURT_ID = "courtId";
    public static final String ARG_COURT_SPORT_ID = "courtSportId";
    public static final String ARG_DATE = "date";
    public static final String ARG_START_TIME = "startTime";
    public static final String ARG_END_TIME = "endTime";

    private final CourtDiscoveryRepository courts;
    private final BookingRepository bookings;
    private final String courtId;
    private final String courtSportId;
    private final String date;
    private final String startTime;
    private final String endTime;
    private final MutableLiveData<UiState<BookingSummary>> summary = new MutableLiveData<>();
    private final MutableLiveData<Boolean> submitting = new MutableLiveData<>(false);
    private final MutableLiveData<AppError> createError = new MutableLiveData<>();
    private final MutableLiveData<Event<String>> created = new MutableLiveData<>();
    private Court court;
    private List<CourtSport> sports;
    private String createdBookingId;
    private Cancellable pendingCourt;
    private Cancellable pendingSports;
    private Cancellable pendingCreate;
    private int loadGeneration;

    @Inject
    public BookingSummaryViewModel(
            CourtDiscoveryRepository courts,
            BookingRepository bookings,
            SavedStateHandle savedStateHandle) {
        this.courts = courts;
        this.bookings = bookings;
        this.courtId = savedStateHandle.get(ARG_COURT_ID);
        this.courtSportId = savedStateHandle.get(ARG_COURT_SPORT_ID);
        this.date = savedStateHandle.get(ARG_DATE);
        this.startTime = savedStateHandle.get(ARG_START_TIME);
        this.endTime = savedStateHandle.get(ARG_END_TIME);
        load();
    }

    public LiveData<UiState<BookingSummary>> getSummary() {
        return summary;
    }

    public LiveData<Boolean> getSubmitting() {
        return submitting;
    }

    /** The last create failure, cleared when a new attempt starts. */
    public LiveData<AppError> getCreateError() {
        return createError;
    }

    /** Fires once with the new booking's id. */
    public LiveData<Event<String>> getCreated() {
        return created;
    }

    public void retry() {
        UiState<BookingSummary> current = summary.getValue();
        if (current != null && current.getStatus() == UiState.Status.ERROR) {
            load();
        }
    }

    /** Creates the Pending booking. Ignored while a request runs or once a booking exists. */
    public void confirm() {
        UiState<BookingSummary> state = summary.getValue();
        if (state == null || state.getStatus() != UiState.Status.SUCCESS || state.getData() == null
                || Boolean.TRUE.equals(submitting.getValue()) || createdBookingId != null) {
            return;
        }
        BookingSummary data = state.getData();
        submitting.setValue(true);
        createError.setValue(null);
        pendingCreate = bookings.createBooking(data.courtSportId, data.startsAtUtcMillis, data.endsAtUtcMillis,
                new ResultCallback<Booking>() {
                    @Override
                    public void onSuccess(Booking booking) {
                        createdBookingId = booking.getId();
                        submitting.setValue(false);
                        created.setValue(new Event<>(booking.getId()));
                    }

                    @Override
                    public void onError(@NonNull AppError error) {
                        submitting.setValue(false);
                        createError.setValue(error);
                    }
                });
    }

    /** Whether a booking has been created from this summary. */
    public boolean isCreated() {
        return createdBookingId != null;
    }

    @Override
    protected void onCleared() {
        cancel(pendingCourt);
        cancel(pendingSports);
        // An in-flight create is not cancelled: the backend may already have created the booking.
    }

    private void load() {
        if (courtId == null || courtSportId == null || date == null || startTime == null || endTime == null) {
            summary.setValue(UiState.error(AppError.of(AppError.Kind.NOT_FOUND)));
            return;
        }
        int generation = ++loadGeneration;
        court = null;
        sports = null;
        summary.setValue(UiState.loading());
        pendingCourt = courts.getCourt(courtId, new ResultCallback<Court>() {
            @Override
            public void onSuccess(Court result) {
                if (generation == loadGeneration) {
                    court = result;
                    onLoaded(generation);
                }
            }

            @Override
            public void onError(@NonNull AppError error) {
                onFailed(generation, error);
            }
        });
        pendingSports = courts.getCourtSports(courtId, new ResultCallback<List<CourtSport>>() {
            @Override
            public void onSuccess(List<CourtSport> result) {
                if (generation == loadGeneration) {
                    sports = result;
                    onLoaded(generation);
                }
            }

            @Override
            public void onError(@NonNull AppError error) {
                onFailed(generation, error);
            }
        });
    }

    private void onLoaded(int generation) {
        UiState<BookingSummary> current = summary.getValue();
        if (generation != loadGeneration || court == null || sports == null
                || (current != null && current.getStatus() == UiState.Status.ERROR)) {
            return;
        }
        CourtSport chosen = null;
        for (CourtSport sport : sports) {
            if (sport.getId().equals(courtSportId)) {
                chosen = sport;
            }
        }
        TimeZone zone = TimeZone.getTimeZone(court.getTimeZoneId());
        Long start = BookingTimes.toInstant(date, startTime, zone);
        Long end = BookingTimes.toInstant(date, endTime, zone);
        if (chosen == null || start == null || end == null) {
            // The sport is no longer offered, or the times cannot be booked at this court.
            summary.setValue(UiState.error(AppError.of(AppError.Kind.NOT_FOUND)));
            return;
        }
        summary.setValue(UiState.success(new BookingSummary(court.getName(), court.getTimeZoneId(), chosen.getId(),
                chosen.getSport().getName(), chosen.getPricePerHour(), chosen.getCurrency(), date, startTime, endTime,
                start, end)));
    }

    private void onFailed(int generation, AppError error) {
        UiState<BookingSummary> current = summary.getValue();
        if (generation != loadGeneration || (current != null && current.getStatus() == UiState.Status.ERROR)) {
            return;
        }
        cancel(pendingCourt);
        cancel(pendingSports);
        summary.setValue(UiState.error(error));
    }

    private static void cancel(Cancellable cancellable) {
        if (cancellable != null) {
            cancellable.cancel();
        }
    }
}
