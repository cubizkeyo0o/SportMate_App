package com.sportmate.app.presentation.courts;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.sportmate.app.R;
import com.sportmate.app.domain.common.AppError;
import com.sportmate.app.domain.model.Court;
import com.sportmate.app.domain.model.CourtSport;
import com.sportmate.app.domain.model.CourtStatus;
import com.sportmate.app.domain.model.OperatingHour;
import com.sportmate.app.domain.model.Sport;
import com.sportmate.app.domain.model.Weekday;
import com.sportmate.app.presentation.common.UiState;
import com.sportmate.app.testutil.ControllableCourtDiscoveryRepository;

import org.junit.Test;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

public class BookingEligibilityTest {
    private static final UiState<Court> ACTIVE = UiState.success(ControllableCourtDiscoveryRepository.court("c-1", "Court A"));
    private static final UiState<List<CourtSport>> SPORTS = UiState.success(Collections.singletonList(
            new CourtSport("cs-1", new Sport("s-1", "Badminton", "badminton"), new BigDecimal("150000"), "VND")));
    private static final UiState<List<OperatingHour>> HOURS = UiState.success(Collections.singletonList(
            new OperatingHour(Weekday.MONDAY, "08:00", "22:00")));

    @Test
    public void activeCourtWithSportsAndHoursCanBeBooked() {
        BookingEligibility eligibility = BookingEligibility.of(ACTIVE, SPORTS, HOURS);

        assertTrue(eligibility.canBook);
        assertEquals(0, eligibility.reason);
    }

    @Test
    public void stillLoadingIsNotBookableWithoutAReason() {
        BookingEligibility courtLoading = BookingEligibility.of(UiState.loading(), SPORTS, HOURS);
        BookingEligibility sportsLoading = BookingEligibility.of(ACTIVE, UiState.loading(), HOURS);

        assertFalse(courtLoading.canBook);
        assertEquals(0, courtLoading.reason);
        assertFalse(sportsLoading.canBook);
        assertEquals(0, sportsLoading.reason);
    }

    @Test
    public void explainsWhyBookingIsUnavailable() {
        Court inactive = new Court("c-1", "Court A", null, "12 Tran Hung Dao", "Hanoi",
                new BigDecimal("21.028511"), new BigDecimal("105.804817"), "Asia/Ho_Chi_Minh", CourtStatus.INACTIVE);

        assertEquals(R.string.book_unavailable_inactive,
                BookingEligibility.of(UiState.success(inactive), SPORTS, HOURS).reason);
        assertEquals(R.string.book_unavailable_no_sports,
                BookingEligibility.of(ACTIVE, UiState.success(Collections.emptyList()), HOURS).reason);
        assertEquals(R.string.book_unavailable_no_hours,
                BookingEligibility.of(ACTIVE, SPORTS, UiState.success(Collections.emptyList())).reason);
        assertEquals(R.string.book_unavailable_not_loaded,
                BookingEligibility.of(ACTIVE, UiState.error(AppError.of(AppError.Kind.NETWORK)), HOURS).reason);
        assertFalse(BookingEligibility.of(ACTIVE, SPORTS, UiState.error(AppError.of(AppError.Kind.SERVER))).canBook);
    }
}
