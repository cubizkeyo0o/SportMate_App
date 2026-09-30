package com.sportmate.app.presentation.courts;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.sportmate.app.domain.common.AppError;
import com.sportmate.app.domain.common.Cancellable;
import com.sportmate.app.domain.common.ResultCallback;
import com.sportmate.app.domain.model.Court;
import com.sportmate.app.domain.repository.CourtDiscoveryRepository;
import com.sportmate.app.presentation.common.UiState;

import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

/** The list of discoverable courts. */
@HiltViewModel
public final class CourtsViewModel extends ViewModel {
    private final CourtDiscoveryRepository repository;
    private final MutableLiveData<UiState<List<Court>>> courts = new MutableLiveData<>();
    private Cancellable pending;

    @Inject
    public CourtsViewModel(CourtDiscoveryRepository repository) {
        this.repository = repository;
        load();
    }

    /** Success with an empty list is the empty state. */
    public LiveData<UiState<List<Court>>> getCourts() {
        return courts;
    }

    public void retry() {
        UiState<List<Court>> current = courts.getValue();
        if (current != null && current.getStatus() == UiState.Status.ERROR) {
            load();
        }
    }

    @Override
    protected void onCleared() {
        if (pending != null) {
            pending.cancel();
        }
    }

    private void load() {
        courts.setValue(UiState.loading());
        pending = repository.getCourts(new ResultCallback<List<Court>>() {
            @Override
            public void onSuccess(List<Court> result) {
                courts.setValue(UiState.success(result));
            }

            @Override
            public void onError(@NonNull AppError error) {
                courts.setValue(UiState.error(error));
            }
        });
    }
}
