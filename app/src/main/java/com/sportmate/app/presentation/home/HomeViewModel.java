package com.sportmate.app.presentation.home;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.sportmate.app.domain.common.AppError;
import com.sportmate.app.domain.common.Cancellable;
import com.sportmate.app.domain.common.ResultCallback;
import com.sportmate.app.domain.repository.HealthRepository;
import com.sportmate.app.presentation.common.UiState;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

/** Demo destination: shows whether the configured backend is reachable. */
@HiltViewModel
public final class HomeViewModel extends ViewModel {
    private final HealthRepository healthRepository;
    private final MutableLiveData<UiState<Boolean>> backendStatus = new MutableLiveData<>();
    private Cancellable pending;

    @Inject
    public HomeViewModel(HealthRepository healthRepository) {
        this.healthRepository = healthRepository;
        checkBackend();
    }

    public LiveData<UiState<Boolean>> getBackendStatus() {
        return backendStatus;
    }

    public void checkBackend() {
        if (pending != null) {
            pending.cancel();
        }
        backendStatus.setValue(UiState.loading());
        pending = healthRepository.checkHealth(new ResultCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean healthy) {
                backendStatus.setValue(UiState.success(healthy));
            }

            @Override
            public void onError(@NonNull AppError error) {
                backendStatus.setValue(UiState.error(error));
            }
        });
    }

    @Override
    protected void onCleared() {
        if (pending != null) {
            pending.cancel();
        }
    }
}
