package com.sportmate.app.presentation.booking;

import androidx.annotation.NonNull;

import java.math.BigDecimal;

/** Everything shown on the summary before a booking is created. */
final class BookingSummary {
    final String courtName;
    final String timeZoneId;
    final String courtSportId;
    final String sportName;
    final BigDecimal pricePerHour;
    final String currency;
    final String date;
    final String startTime;
    final String endTime;
    final long startsAtUtcMillis;
    final long endsAtUtcMillis;
    final long minutes;
    /** Preview only; the backend's {@code total_price} is authoritative. */
    final BigDecimal estimatedTotal;

    BookingSummary(
            @NonNull String courtName,
            @NonNull String timeZoneId,
            @NonNull String courtSportId,
            @NonNull String sportName,
            @NonNull BigDecimal pricePerHour,
            @NonNull String currency,
            @NonNull String date,
            @NonNull String startTime,
            @NonNull String endTime,
            long startsAtUtcMillis,
            long endsAtUtcMillis) {
        this.courtName = courtName;
        this.timeZoneId = timeZoneId;
        this.courtSportId = courtSportId;
        this.sportName = sportName;
        this.pricePerHour = pricePerHour;
        this.currency = currency;
        this.date = date;
        this.startTime = startTime;
        this.endTime = endTime;
        this.startsAtUtcMillis = startsAtUtcMillis;
        this.endsAtUtcMillis = endsAtUtcMillis;
        // Elapsed minutes between the instants, the same basis the backend prices on.
        this.minutes = (endsAtUtcMillis - startsAtUtcMillis) / 60_000L;
        this.estimatedTotal = BookingFormats.estimate(pricePerHour, minutes);
    }
}
