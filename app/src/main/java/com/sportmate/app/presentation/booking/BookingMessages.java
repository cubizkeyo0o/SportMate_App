package com.sportmate.app.presentation.booking;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.sportmate.app.R;
import com.sportmate.app.domain.common.AppError;
import com.sportmate.app.presentation.common.ErrorMessages;
import com.sportmate.app.presentation.common.UiText;

import java.util.List;
import java.util.Map;

/**
 * User-facing text for booking failures. Booking rule violations come back as validation errors
 * with a client-facing message (e.g. under {@code starts_at} or {@code status}), which is shown
 * as is; other failures use the app's common error text.
 */
final class BookingMessages {
    private BookingMessages() {
    }

    /** Creating a booking. */
    @NonNull
    static UiText forCreate(@NonNull AppError error) {
        switch (error.getKind()) {
            case CONFLICT:
                return UiText.of(R.string.booking_conflict);
            case NOT_FOUND:
                return UiText.of(R.string.booking_sport_unavailable);
            default:
                return fieldMessageOrCommon(error);
        }
    }

    /** Loading, paying for or cancelling an existing booking. */
    @NonNull
    static UiText forBooking(@NonNull AppError error) {
        if (error.getKind() == AppError.Kind.NOT_FOUND) {
            return UiText.of(R.string.booking_not_found);
        }
        return fieldMessageOrCommon(error);
    }

    private static UiText fieldMessageOrCommon(AppError error) {
        if (error.getKind() == AppError.Kind.VALIDATION) {
            String message = firstFieldMessage(error.getFieldErrors());
            if (message != null) {
                return UiText.raw(message);
            }
        }
        return ErrorMessages.toUiText(error);
    }

    @Nullable
    private static String firstFieldMessage(Map<String, List<String>> fieldErrors) {
        for (List<String> messages : fieldErrors.values()) {
            if (messages != null && !messages.isEmpty()) {
                return messages.get(0);
            }
        }
        return null;
    }
}
