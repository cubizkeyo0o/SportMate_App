package com.sportmate.app.presentation.courts;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.lifecycle.SavedStateHandle;

import com.sportmate.app.domain.common.AppError;
import com.sportmate.app.domain.model.Court;
import com.sportmate.app.domain.model.CourtSport;
import com.sportmate.app.domain.model.OperatingHour;
import com.sportmate.app.domain.model.Sport;
import com.sportmate.app.domain.model.Weekday;
import com.sportmate.app.presentation.common.UiState;
import com.sportmate.app.testutil.ControllableCourtDiscoveryRepository;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CourtDetailViewModelTest {
    private static final String COURT_ID = "c-1";

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    private final ControllableCourtDiscoveryRepository repository = new ControllableCourtDiscoveryRepository();
    private CourtDetailViewModel viewModel;

    @Before
    public void setUp() {
        viewModel = viewModelFor(COURT_ID);
    }

    @Test
    public void loadsCourtSportsAndHoursForTheArgument() {
        assertEquals(COURT_ID, repository.lastCourtId);
        assertEquals(1, repository.courtCalls);
        assertEquals(1, repository.sportsCalls);
        assertEquals(1, repository.hoursCalls);
        assertEquals(UiState.Status.LOADING, viewModel.getCourt().getValue().getStatus());
        assertEquals(UiState.Status.LOADING, viewModel.getSports().getValue().getStatus());
        assertEquals(UiState.Status.LOADING, viewModel.getOperatingHours().getValue().getStatus());
    }

    @Test
    public void courtLoadsIndependentlyOfSections() {
        Court court = ControllableCourtDiscoveryRepository.court(COURT_ID, "Court A");

        repository.courtCallback.onSuccess(court);

        assertEquals(UiState.Status.SUCCESS, viewModel.getCourt().getValue().getStatus());
        assertSame(court, viewModel.getCourt().getValue().getData());
        assertEquals(UiState.Status.LOADING, viewModel.getSports().getValue().getStatus());
    }

    @Test
    public void notFoundCourtIsAnErrorState() {
        repository.courtCallback.onError(new AppError(AppError.Kind.NOT_FOUND, 404, "Court not found.", null));

        UiState<Court> state = viewModel.getCourt().getValue();
        assertEquals(UiState.Status.ERROR, state.getStatus());
        assertEquals(AppError.Kind.NOT_FOUND, state.getError().getKind());
    }

    @Test
    public void sportsFailureLeavesCourtAndHoursIntactAndRetriesAlone() {
        repository.courtCallback.onSuccess(ControllableCourtDiscoveryRepository.court(COURT_ID, "Court A"));
        repository.hoursCallback.onSuccess(Collections.singletonList(new OperatingHour(Weekday.MONDAY, "08:00", "22:00")));

        repository.sportsCallback.onError(new AppError(AppError.Kind.SERVER, 500, null, null));

        assertEquals(UiState.Status.SUCCESS, viewModel.getCourt().getValue().getStatus());
        assertEquals(UiState.Status.SUCCESS, viewModel.getOperatingHours().getValue().getStatus());
        assertEquals(UiState.Status.ERROR, viewModel.getSports().getValue().getStatus());

        viewModel.retrySports();

        assertEquals(2, repository.sportsCalls);
        assertEquals(1, repository.courtCalls);
        assertEquals(1, repository.hoursCalls);
        List<CourtSport> sports = Collections.singletonList(
                new CourtSport(new Sport("s-1", "Badminton", "badminton"), new BigDecimal("150000.00"), "VND"));
        repository.sportsCallback.onSuccess(sports);
        assertSame(sports, viewModel.getSports().getValue().getData());
    }

    @Test
    public void hoursFailureLeavesCourtAndSportsIntactAndRetriesAlone() {
        repository.courtCallback.onSuccess(ControllableCourtDiscoveryRepository.court(COURT_ID, "Court A"));
        repository.sportsCallback.onSuccess(Collections.emptyList());

        repository.hoursCallback.onError(AppError.of(AppError.Kind.NETWORK));

        assertEquals(UiState.Status.SUCCESS, viewModel.getCourt().getValue().getStatus());
        assertEquals(UiState.Status.SUCCESS, viewModel.getSports().getValue().getStatus());
        assertEquals(UiState.Status.ERROR, viewModel.getOperatingHours().getValue().getStatus());

        viewModel.retryOperatingHours();

        assertEquals(2, repository.hoursCalls);
        assertEquals(1, repository.sportsCalls);
        repository.hoursCallback.onSuccess(Collections.emptyList());
        assertTrue(viewModel.getOperatingHours().getValue().getData().isEmpty());
    }

    @Test
    public void retryReloadsOnlyWhatFailed() {
        repository.courtCallback.onError(AppError.of(AppError.Kind.NETWORK));
        repository.sportsCallback.onSuccess(Collections.emptyList());
        repository.hoursCallback.onError(AppError.of(AppError.Kind.NETWORK));

        viewModel.retry();

        assertEquals(2, repository.courtCalls);
        assertEquals(1, repository.sportsCalls);
        assertEquals(2, repository.hoursCalls);
    }

    @Test
    public void sectionRetryIsIgnoredUnlessFailed() {
        viewModel.retrySports();
        viewModel.retryOperatingHours();

        assertEquals(1, repository.sportsCalls);
        assertEquals(1, repository.hoursCalls);
    }

    @Test
    public void missingArgumentIsNotFoundWithoutCallingBackend() {
        ControllableCourtDiscoveryRepository fresh = new ControllableCourtDiscoveryRepository();
        CourtDetailViewModel noArg = new CourtDetailViewModel(fresh, new SavedStateHandle());

        assertEquals(AppError.Kind.NOT_FOUND, noArg.getCourt().getValue().getError().getKind());
        assertEquals(0, fresh.courtCalls + fresh.sportsCalls + fresh.hoursCalls);
    }

    @Test
    public void clearingCancelsAllPendingRequests() {
        viewModel.onCleared();

        assertEquals(3, repository.cancelled);
    }

    private CourtDetailViewModel viewModelFor(String courtId) {
        Map<String, Object> args = new HashMap<>();
        args.put(CourtDetailViewModel.ARG_COURT_ID, courtId);
        return new CourtDetailViewModel(repository, new SavedStateHandle(args));
    }
}
