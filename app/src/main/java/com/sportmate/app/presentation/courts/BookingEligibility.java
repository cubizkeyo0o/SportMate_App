package com.sportmate.app.presentation.courts;

import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import com.sportmate.app.R;
import com.sportmate.app.domain.model.Court;
import com.sportmate.app.domain.model.CourtSport;
import com.sportmate.app.domain.model.CourtStatus;
import com.sportmate.app.domain.model.OperatingHour;
import com.sportmate.app.presentation.common.UiState;

import java.util.List;

/**
 * Whether booking can be started from the court detail screen, and if not, why. This only avoids
 * starting a booking that cannot succeed; the backend still validates the booking itself.
 */
final class BookingEligibility {
    final boolean canBook;
    /** Why booking is unavailable, or 0 when it is available or still being checked. */
    @StringRes
    final int reason;

    private BookingEligibility(boolean canBook, @StringRes int reason) {
        this.canBook = canBook;
        this.reason = reason;
    }

    static BookingEligibility of(
            @Nullable UiState<Court> court,
            @Nullable UiState<List<CourtSport>> sports,
            @Nullable UiState<List<OperatingHour>> hours) {
        if (!loaded(court) || court.getData() == null) {
            return new BookingEligibility(false, 0);
        }
        if (court.getData().getStatus() != CourtStatus.ACTIVE) {
            return new BookingEligibility(false, R.string.book_unavailable_inactive);
        }
        if (failed(sports) || failed(hours)) {
            return new BookingEligibility(false, R.string.book_unavailable_not_loaded);
        }
        if (!loaded(sports) || !loaded(hours)) {
            return new BookingEligibility(false, 0);
        }
        if (isEmpty(sports.getData())) {
            return new BookingEligibility(false, R.string.book_unavailable_no_sports);
        }
        if (isEmpty(hours.getData())) {
            return new BookingEligibility(false, R.string.book_unavailable_no_hours);
        }
        return new BookingEligibility(true, 0);
    }

    private static boolean loaded(@Nullable UiState<?> state) {
        return state != null && state.getStatus() == UiState.Status.SUCCESS;
    }

    private static boolean failed(@Nullable UiState<?> state) {
        return state != null && state.getStatus() == UiState.Status.ERROR;
    }

    private static boolean isEmpty(@Nullable List<?> list) {
        return list == null || list.isEmpty();
    }
}
