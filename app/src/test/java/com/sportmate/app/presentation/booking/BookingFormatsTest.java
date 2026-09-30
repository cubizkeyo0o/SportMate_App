package com.sportmate.app.presentation.booking;

import static org.junit.Assert.assertEquals;

import com.sportmate.app.R;
import com.sportmate.app.domain.common.AppError;
import com.sportmate.app.domain.model.BookingStatus;
import com.sportmate.app.presentation.common.UiText;

import org.junit.Test;

import java.math.BigDecimal;
import java.text.DateFormat;
import java.util.Collections;
import java.util.Locale;
import java.util.TimeZone;

/** Booking formatting and error texts. */
public class BookingFormatsTest {
    private static final long START_UTC = 4_094_852_400_000L; // 2099-10-05T03:00:00Z

    @Test
    public void estimateFollowsTheBackendRule() {
        assertEquals(new BigDecimal("225000.00"), BookingFormats.estimate(new BigDecimal("150000.00"), 90));
        assertEquals(new BigDecimal("300000.00"), BookingFormats.estimate(new BigDecimal("150000"), 120));
        assertEquals(new BigDecimal("0.00"), BookingFormats.estimate(BigDecimal.ZERO, 60));
        // Rounded to 2 decimals with halves away from zero: 0.30 * 5 / 60 = 0.025.
        assertEquals(new BigDecimal("0.03"), BookingFormats.estimate(new BigDecimal("0.30"), 5));
        assertEquals(new BigDecimal("16.67"), BookingFormats.estimate(new BigDecimal("10.00"), 100));
    }

    @Test
    public void plainDatesNeverShiftWithThePhoneZone() {
        TimeZone original = TimeZone.getDefault();
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("Pacific/Honolulu"));
            assertEquals("Oct 5, 2099", BookingFormats.date("2099-10-05", DateFormat.MEDIUM, Locale.US));
            assertEquals("Monday, October 5, 2099", BookingFormats.date("2099-10-05", DateFormat.FULL, Locale.US));
        } finally {
            TimeZone.setDefault(original);
        }
    }

    @Test
    public void instantsAreShownInTheCourtZone() {
        assertEquals("Oct 5, 2099 10:00", BookingFormats.courtDateTime(START_UTC, "Asia/Ho_Chi_Minh", Locale.US));
        assertEquals("Oct 5, 2099 03:00", BookingFormats.courtDateTime(START_UTC, "UTC", Locale.US));
    }

    @Test
    public void statusLabels() {
        assertEquals(R.string.booking_status_pending, BookingFormats.statusLabel(BookingStatus.PENDING));
        assertEquals(R.string.booking_status_confirmed, BookingFormats.statusLabel(BookingStatus.CONFIRMED));
        assertEquals(R.string.booking_status_cancelled, BookingFormats.statusLabel(BookingStatus.CANCELLED));
        assertEquals(R.string.booking_status_completed, BookingFormats.statusLabel(BookingStatus.COMPLETED));
        assertEquals(R.string.booking_status_no_show, BookingFormats.statusLabel(BookingStatus.NO_SHOW));
        assertEquals(R.string.value_unknown, BookingFormats.statusLabel(BookingStatus.UNKNOWN));
    }

    @Test
    public void createErrorTexts() {
        assertEquals(UiText.of(R.string.booking_conflict), BookingMessages.forCreate(
                new AppError(AppError.Kind.CONFLICT, 409, "The court is already booked for part of this time.", null)));
        assertEquals(UiText.of(R.string.booking_sport_unavailable), BookingMessages.forCreate(
                new AppError(AppError.Kind.NOT_FOUND, 404, "Court sport not found.", null)));
        assertEquals(UiText.raw("The court has no operating hours on Sunday."), BookingMessages.forCreate(
                new AppError(AppError.Kind.VALIDATION, 400, "One or more validation errors occurred.",
                        Collections.singletonMap("starts_at",
                                Collections.singletonList("The court has no operating hours on Sunday.")))));
        assertEquals(UiText.of(R.string.error_network), BookingMessages.forCreate(AppError.of(AppError.Kind.NETWORK)));
        assertEquals(UiText.of(R.string.error_server),
                BookingMessages.forCreate(new AppError(AppError.Kind.SERVER, 500, "An unexpected error occurred.", null)));
        assertEquals(UiText.of(R.string.error_unauthorized),
                BookingMessages.forCreate(new AppError(AppError.Kind.UNAUTHORIZED, 401, null, null)));
    }

    @Test
    public void bookingErrorTexts() {
        assertEquals(UiText.of(R.string.booking_not_found),
                BookingMessages.forBooking(new AppError(AppError.Kind.NOT_FOUND, 404, "Booking not found.", null)));
        assertEquals(UiText.raw("This booking has already been paid and confirmed."), BookingMessages.forBooking(
                new AppError(AppError.Kind.VALIDATION, 400, "One or more validation errors occurred.",
                        Collections.singletonMap("status",
                                Collections.singletonList("This booking has already been paid and confirmed.")))));
        assertEquals(UiText.raw("Only the person who made the booking can pay for it."), BookingMessages.forBooking(
                new AppError(AppError.Kind.FORBIDDEN, 403, "Only the person who made the booking can pay for it.", null)));
    }
}
