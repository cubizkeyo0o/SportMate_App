package com.sportmate.app.presentation.courts;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import com.sportmate.app.domain.common.AppError;
import com.sportmate.app.domain.model.Court;
import com.sportmate.app.presentation.common.UiState;
import com.sportmate.app.testutil.ControllableCourtDiscoveryRepository;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class CourtsViewModelTest {
    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    private final ControllableCourtDiscoveryRepository repository = new ControllableCourtDiscoveryRepository();
    private CourtsViewModel viewModel;

    @Before
    public void setUp() {
        viewModel = new CourtsViewModel(repository);
    }

    @Test
    public void loadingThenContent() {
        assertEquals(UiState.Status.LOADING, status());
        List<Court> courts = Arrays.asList(
                ControllableCourtDiscoveryRepository.court("c-1", "Court A"),
                ControllableCourtDiscoveryRepository.court("c-2", "Court B"));

        repository.courtsCallback.onSuccess(courts);

        assertEquals(UiState.Status.SUCCESS, status());
        assertSame(courts, viewModel.getCourts().getValue().getData());
    }

    @Test
    public void loadingThenEmpty() {
        repository.courtsCallback.onSuccess(Collections.emptyList());

        assertEquals(UiState.Status.SUCCESS, status());
        assertTrue(viewModel.getCourts().getValue().getData().isEmpty());
    }

    @Test
    public void loadingThenErrorAndRetryLoadsAgain() {
        AppError error = AppError.of(AppError.Kind.NETWORK);
        repository.courtsCallback.onError(error);

        assertEquals(UiState.Status.ERROR, status());
        assertSame(error, viewModel.getCourts().getValue().getError());

        viewModel.retry();

        assertEquals(UiState.Status.LOADING, status());
        assertEquals(2, repository.courtsCalls);
        repository.courtsCallback.onSuccess(Collections.emptyList());
        assertEquals(UiState.Status.SUCCESS, status());
    }

    @Test
    public void retryIsIgnoredWhileLoadingOrLoaded() {
        viewModel.retry();
        assertEquals(1, repository.courtsCalls);

        repository.courtsCallback.onSuccess(Collections.emptyList());
        viewModel.retry();
        assertEquals(1, repository.courtsCalls);
    }

    @Test
    public void clearingCancelsPendingLoad() {
        viewModel.onCleared();

        assertEquals(1, repository.cancelled);
    }

    private UiState.Status status() {
        return viewModel.getCourts().getValue().getStatus();
    }
}
