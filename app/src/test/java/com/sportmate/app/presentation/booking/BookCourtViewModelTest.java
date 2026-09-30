package com.sportmate.app.presentation.booking;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.lifecycle.SavedStateHandle;

import com.sportmate.app.R;
import com.sportmate.app.domain.common.AppError;
import com.sportmate.app.domain.model.CourtSport;
import com.sportmate.app.domain.model.OperatingHour;
import com.sportmate.app.domain.model.Sport;
import com.sportmate.app.domain.model.Weekday;
import com.sportmate.app.presentation.common.Event;
import com.sportmate.app.presentation.common.UiState;
import com.sportmate.app.presentation.common.UiText;
import com.sportmate.app.testutil.ControllableCourtDiscoveryRepository;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class BookCourtViewModelTest {
    private static final CourtSport BADMINTON = new CourtSport("cs-1", new Sport("s-1", "Badminton", "badminton"),
            new BigDecimal("150000"), "VND");
    private static final CourtSport TENNIS = new CourtSport("cs-2", new Sport("s-2", "Tennis", "tennis"),
            new BigDecimal("200000"), "VND");
    // 2099-10-05 is a Monday.
    private static final List<OperatingHour> HOURS =
            Collections.singletonList(new OperatingHour(Weekday.MONDAY, "08:00", "22:00"));

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    private final ControllableCourtDiscoveryRepository repository = new ControllableCourtDiscoveryRepository();
    private BookCourtViewModel viewModel;

    @Before
    public void setUp() {
        viewModel = new BookCourtViewModel(repository,
                new SavedStateHandle(Collections.singletonMap(BookCourtViewModel.ARG_COURT_ID, "c-1")));
    }

    @Test
    public void loadsCourtSportsAndHoursForTheArgument() {
        assertEquals("c-1", repository.lastCourtId);
        assertEquals(UiState.Status.LOADING, status());

        loaded(Arrays.asList(BADMINTON, TENNIS));

        assertEquals(UiState.Status.SUCCESS, status());
        assertEquals(2, viewModel.getOptions().getValue().getData().sports.size());
        assertNull(viewModel.getSelection().getValue().courtSportId);
    }

    @Test
    public void singleSportIsPreselected() {
        loaded(Collections.singletonList(BADMINTON));

        assertEquals("cs-1", viewModel.getSelection().getValue().courtSportId);
    }

    @Test
    public void anyFailedPartIsAnErrorAndRetryReloads() {
        repository.sportsCallback.onError(AppError.of(AppError.Kind.NETWORK));
        assertEquals(UiState.Status.ERROR, status());

        viewModel.retry();

        assertEquals(UiState.Status.LOADING, status());
        assertEquals(2, repository.sportsCalls);
        assertEquals(2, repository.courtCalls);
        assertEquals(2, repository.hoursCalls);
    }

    @Test
    public void validSelectionOpensTheSummaryOnceWithCourtSportId() {
        loaded(Arrays.asList(BADMINTON, TENNIS));
        viewModel.selectSport("cs-2");
        viewModel.selectDate("2099-10-05");
        viewModel.selectStartTime("10:00");
        viewModel.selectEndTime("11:30");

        viewModel.continueToSummary();

        assertNull(viewModel.getError().getValue());
        Event<BookingSelection> event = viewModel.getShowSummary().getValue();
        BookingSelection selection = event.getContentIfNotHandled();
        assertEquals("cs-2", selection.courtSportId);
        assertEquals("2099-10-05", selection.date);
        assertEquals("10:00", selection.startTime);
        assertEquals("11:30", selection.endTime);
        assertNull(event.getContentIfNotHandled());
    }

    @Test
    public void invalidSelectionShowsAnErrorUntilChanged() {
        loaded(Arrays.asList(BADMINTON, TENNIS));
        viewModel.selectSport("cs-1");
        viewModel.selectDate("2099-10-05");
        viewModel.selectStartTime("21:00");
        viewModel.selectEndTime("23:00");

        viewModel.continueToSummary();

        assertEquals(UiText.of(R.string.booking_error_outside_hours), viewModel.getError().getValue());
        assertNull(viewModel.getShowSummary().getValue());

        viewModel.selectEndTime("22:00");
        assertNull(viewModel.getError().getValue());
    }

    @Test
    public void continueIsIgnoredUntilLoaded() {
        viewModel.continueToSummary();

        assertNull(viewModel.getError().getValue());
        assertNull(viewModel.getShowSummary().getValue());
    }

    private void loaded(List<CourtSport> sports) {
        repository.courtCallback.onSuccess(ControllableCourtDiscoveryRepository.court("c-1", "Court A"));
        repository.sportsCallback.onSuccess(sports);
        repository.hoursCallback.onSuccess(HOURS);
    }

    private UiState.Status status() {
        return viewModel.getOptions().getValue().getStatus();
    }
}
