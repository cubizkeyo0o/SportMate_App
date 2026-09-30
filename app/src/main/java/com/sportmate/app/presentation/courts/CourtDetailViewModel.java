package com.sportmate.app.presentation.courts;

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
import com.sportmate.app.presentation.common.UiState;

import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

/**
 * One court's details. The court, its sports and its operating hours load in parallel, each with
 * its own state, so a failed section does not hide the rest of the screen and can be retried alone.
 */
@HiltViewModel
public final class CourtDetailViewModel extends ViewModel {
    /** Navigation argument: the court to show. */
    public static final String ARG_COURT_ID = "courtId";

    private final CourtDiscoveryRepository repository;
    private final String courtId;
    private final MutableLiveData<UiState<Court>> court = new MutableLiveData<>();
    private final MutableLiveData<UiState<List<CourtSport>>> sports = new MutableLiveData<>();
    private final MutableLiveData<UiState<List<OperatingHour>>> hours = new MutableLiveData<>();
    private Cancellable pendingCourt;
    private Cancellable pendingSports;
    private Cancellable pendingHours;

    @Inject
    public CourtDetailViewModel(CourtDiscoveryRepository repository, SavedStateHandle savedStateHandle) {
        this.repository = repository;
        this.courtId = savedStateHandle.get(ARG_COURT_ID);
        loadCourt();
        loadSports();
        loadHours();
    }

    /** NOT_FOUND means the court is inactive or does not exist. */
    public LiveData<UiState<Court>> getCourt() {
        return court;
    }

    /** Success with an empty list means the court offers no sports yet. */
    public LiveData<UiState<List<CourtSport>>> getSports() {
        return sports;
    }

    /** Success with an empty list means no operating hours are configured. */
    public LiveData<UiState<List<OperatingHour>>> getOperatingHours() {
        return hours;
    }

    /** Reloads the court and any section that failed. */
    public void retry() {
        if (failed(court)) {
            loadCourt();
        }
        retrySports();
        retryOperatingHours();
    }

    public void retrySports() {
        if (failed(sports)) {
            loadSports();
        }
    }

    public void retryOperatingHours() {
        if (failed(hours)) {
            loadHours();
        }
    }

    @Override
    protected void onCleared() {
        cancel(pendingCourt);
        cancel(pendingSports);
        cancel(pendingHours);
    }

    private void loadCourt() {
        if (courtId == null) {
            court.setValue(UiState.error(AppError.of(AppError.Kind.NOT_FOUND)));
            return;
        }
        court.setValue(UiState.loading());
        pendingCourt = repository.getCourt(courtId, new StateCallback<>(court));
    }

    private void loadSports() {
        if (courtId == null) {
            sports.setValue(UiState.error(AppError.of(AppError.Kind.NOT_FOUND)));
            return;
        }
        sports.setValue(UiState.loading());
        pendingSports = repository.getCourtSports(courtId, new StateCallback<>(sports));
    }

    private void loadHours() {
        if (courtId == null) {
            hours.setValue(UiState.error(AppError.of(AppError.Kind.NOT_FOUND)));
            return;
        }
        hours.setValue(UiState.loading());
        pendingHours = repository.getOperatingHours(courtId, new StateCallback<>(hours));
    }

    private static boolean failed(LiveData<? extends UiState<?>> state) {
        UiState<?> value = state.getValue();
        return value != null && value.getStatus() == UiState.Status.ERROR;
    }

    private static void cancel(Cancellable cancellable) {
        if (cancellable != null) {
            cancellable.cancel();
        }
    }

    /** Publishes a repository result as a UiState. */
    private static final class StateCallback<T> implements ResultCallback<T> {
        private final MutableLiveData<UiState<T>> target;

        StateCallback(MutableLiveData<UiState<T>> target) {
            this.target = target;
        }

        @Override
        public void onSuccess(T result) {
            target.setValue(UiState.success(result));
        }

        @Override
        public void onError(@NonNull AppError error) {
            target.setValue(UiState.error(error));
        }
    }
}
