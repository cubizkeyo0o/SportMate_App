package com.sportmate.app.presentation.home;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import com.sportmate.app.domain.common.AppError;
import com.sportmate.app.domain.common.Cancellable;
import com.sportmate.app.domain.common.ResultCallback;
import com.sportmate.app.domain.repository.HealthRepository;
import com.sportmate.app.presentation.common.UiState;

import org.junit.Rule;
import org.junit.Test;

public class HomeViewModelTest {
    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    private final ControllableHealthRepository repository = new ControllableHealthRepository();

    @Test
    public void startsLoadingThenShowsSuccess() {
        HomeViewModel viewModel = new HomeViewModel(repository);
        assertEquals(UiState.Status.LOADING, viewModel.getBackendStatus().getValue().getStatus());

        repository.callback.onSuccess(true);

        UiState<Boolean> state = viewModel.getBackendStatus().getValue();
        assertEquals(UiState.Status.SUCCESS, state.getStatus());
        assertTrue(state.getData());
    }

    @Test
    public void exposesRepositoryError() {
        HomeViewModel viewModel = new HomeViewModel(repository);
        AppError error = AppError.of(AppError.Kind.NETWORK);

        repository.callback.onError(error);

        UiState<Boolean> state = viewModel.getBackendStatus().getValue();
        assertEquals(UiState.Status.ERROR, state.getStatus());
        assertSame(error, state.getError());
    }

    @Test
    public void retryCancelsPreviousCallAndClearCancelsPending() {
        HomeViewModel viewModel = new HomeViewModel(repository);

        viewModel.checkBackend();
        assertEquals(1, repository.cancelled);

        viewModel.onCleared();
        assertEquals(2, repository.cancelled);
    }

    private static final class ControllableHealthRepository implements HealthRepository {
        ResultCallback<Boolean> callback;
        int cancelled;

        @Override
        public Cancellable checkHealth(ResultCallback<Boolean> callback) {
            this.callback = callback;
            return () -> cancelled++;
        }
    }
}
