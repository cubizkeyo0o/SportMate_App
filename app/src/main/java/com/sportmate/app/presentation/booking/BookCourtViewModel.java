package com.sportmate.app.presentation.booking;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;

import com.sportmate.app.domain.common.AppError;
import com.sportmate.app.domain.common.Cancellable;
import com.sportmate.app.domain.common.ResultCallback;
import com.sportmate.app.domain.model.Court;
import com.sportmate.app.domain.model.CourtSport;
import com.sportmate.app.domain.model.OperatingHour;
import com.sportmate.app.domain.repository.CourtDiscoveryRepository;
import com.sportmate.app.presentation.common.Event;
import com.sportmate.app.presentation.common.UiState;
import com.sportmate.app.presentation.common.UiText;

import java.util.List;
import java.util.TimeZone;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

/**
 * Choosing a sport, a date and court-local start and end times. Loads the court (for its time
 * zone), its sports and its operating hours from court discovery.
 */
@HiltViewModel
public final class BookCourtViewModel extends ViewModel {
    /** Navigation argument: the court to book. */
    public static final String ARG_COURT_ID = "courtId";

    /** The court, its sports and its hours, needed together to book. */
    static final class Options {
        final Court court;
        final List<CourtSport> sports;
        final List<OperatingHour> hours;

        Options(Court court, List<CourtSport> sports, List<OperatingHour> hours) {
            this.court = court;
            this.sports = sports;
            this.hours = hours;
        }
    }

    private final CourtDiscoveryRepository repository;
    private final String courtId;
    private final MutableLiveData<UiState<Options>> options = new MutableLiveData<>();
    private final MutableLiveData<BookingSelection> selection = new MutableLiveData<>(BookingSelection.empty());
    private final MutableLiveData<UiText> error = new MutableLiveData<>();
    private final MutableLiveData<Event<BookingSelection>> showSummary = new MutableLiveData<>();
    private Cancellable pendingCourt;
    private Cancellable pendingSports;
    private Cancellable pendingHours;
    private int loadGeneration;

    @Inject
    public BookCourtViewModel(CourtDiscoveryRepository repository, SavedStateHandle savedStateHandle) {
        this.repository = repository;
        this.courtId = savedStateHandle.get(ARG_COURT_ID);
        load();
    }

    public String getCourtId() {
        return courtId;
    }

    public LiveData<UiState<Options>> getOptions() {
        return options;
    }

    public LiveData<BookingSelection> getSelection() {
        return selection;
    }

    /** Why the selection cannot continue yet, or null. */
    public LiveData<UiText> getError() {
        return error;
    }

    /** Fires once with a selection that passed the client-side checks. */
    public LiveData<Event<BookingSelection>> getShowSummary() {
        return showSummary;
    }

    public void retry() {
        UiState<Options> current = options.getValue();
        if (current != null && current.getStatus() == UiState.Status.ERROR) {
            load();
        }
    }

    public void selectSport(String courtSportId) {
        update(current().withCourtSport(courtSportId));
    }

    public void selectDate(String date) {
        update(current().withDate(date));
    }

    public void selectStartTime(String time) {
        update(current().withStartTime(time));
    }

    public void selectEndTime(String time) {
        update(current().withEndTime(time));
    }

    public void continueToSummary() {
        UiState<Options> state = options.getValue();
        if (state == null || state.getStatus() != UiState.Status.SUCCESS || state.getData() == null) {
            return;
        }
        Options loaded = state.getData();
        UiText problem = BookingTimes.validate(current(),
                TimeZone.getTimeZone(loaded.court.getTimeZoneId()), loaded.hours, System.currentTimeMillis());
        error.setValue(problem);
        if (problem == null) {
            showSummary.setValue(new Event<>(current()));
        }
    }

    @Override
    protected void onCleared() {
        cancel(pendingCourt);
        cancel(pendingSports);
        cancel(pendingHours);
    }

    private void update(BookingSelection value) {
        selection.setValue(value);
        error.setValue(null);
    }

    private BookingSelection current() {
        BookingSelection value = selection.getValue();
        return value == null ? BookingSelection.empty() : value;
    }

    private void load() {
        if (courtId == null) {
            options.setValue(UiState.error(AppError.of(AppError.Kind.NOT_FOUND)));
            return;
        }
        int generation = ++loadGeneration;
        options.setValue(UiState.loading());
        Parts parts = new Parts();
        pendingCourt = repository.getCourt(courtId, new PartCallback<Court>(generation, parts) {
            @Override
            void store(Court result) {
                parts.court = result;
            }
        });
        pendingSports = repository.getCourtSports(courtId, new PartCallback<List<CourtSport>>(generation, parts) {
            @Override
            void store(List<CourtSport> result) {
                parts.sports = result;
            }
        });
        pendingHours = repository.getOperatingHours(courtId, new PartCallback<List<OperatingHour>>(generation, parts) {
            @Override
            void store(List<OperatingHour> result) {
                parts.hours = result;
            }
        });
    }

    private void onPartLoaded(int generation, Parts parts) {
        if (generation != loadGeneration || parts.failed
                || parts.court == null || parts.sports == null || parts.hours == null) {
            return;
        }
        options.setValue(UiState.success(new Options(parts.court, parts.sports, parts.hours)));
        // With a single sport there is nothing to choose.
        if (parts.sports.size() == 1 && current().courtSportId == null) {
            selection.setValue(current().withCourtSport(parts.sports.get(0).getId()));
        }
    }

    private void onPartFailed(int generation, Parts parts, AppError failure) {
        if (generation != loadGeneration || parts.failed) {
            return;
        }
        parts.failed = true;
        cancel(pendingCourt);
        cancel(pendingSports);
        cancel(pendingHours);
        options.setValue(UiState.error(failure));
    }

    private static void cancel(Cancellable cancellable) {
        if (cancellable != null) {
            cancellable.cancel();
        }
    }

    private static final class Parts {
        Court court;
        List<CourtSport> sports;
        List<OperatingHour> hours;
        boolean failed;
    }

    private abstract class PartCallback<T> implements ResultCallback<T> {
        private final int generation;
        private final Parts parts;

        PartCallback(int generation, Parts parts) {
            this.generation = generation;
            this.parts = parts;
        }

        abstract void store(T result);

        @Override
        public void onSuccess(T result) {
            if (generation == loadGeneration && !parts.failed) {
                store(result);
                onPartLoaded(generation, parts);
            }
        }

        @Override
        public void onError(@NonNull AppError failure) {
            onPartFailed(generation, parts, failure);
        }
    }
}
