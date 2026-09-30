package com.sportmate.app.domain.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.math.BigDecimal;

/**
 * A booking as the backend reports it. Start and end are available both as UTC instants and as the
 * court's local wall-clock date and times; the latter are what the user picked and should be shown.
 */
public final class Booking {
    private final String id;
    private final String courtId;
    private final String courtSportId;
    private final Sport sport;
    private final long startsAtUtcMillis;
    private final long endsAtUtcMillis;
    private final String timeZoneId;
    private final String localDate;
    private final String localStartTime;
    private final String localEndTime;
    private final BookingStatus status;
    private final BigDecimal totalPrice;
    private final String currency;
    @Nullable
    private final Long holdExpiresAtUtcMillis;
    @Nullable
    private final String cancellationReason;
    @Nullable
    private final Long cancelledAtUtcMillis;
    private final long createdAtUtcMillis;

    public Booking(
            @NonNull String id,
            @NonNull String courtId,
            @NonNull String courtSportId,
            @NonNull Sport sport,
            long startsAtUtcMillis,
            long endsAtUtcMillis,
            @NonNull String timeZoneId,
            @NonNull String localDate,
            @NonNull String localStartTime,
            @NonNull String localEndTime,
            @NonNull BookingStatus status,
            @NonNull BigDecimal totalPrice,
            @NonNull String currency,
            @Nullable Long holdExpiresAtUtcMillis,
            @Nullable String cancellationReason,
            @Nullable Long cancelledAtUtcMillis,
            long createdAtUtcMillis) {
        this.id = id;
        this.courtId = courtId;
        this.courtSportId = courtSportId;
        this.sport = sport;
        this.startsAtUtcMillis = startsAtUtcMillis;
        this.endsAtUtcMillis = endsAtUtcMillis;
        this.timeZoneId = timeZoneId;
        this.localDate = localDate;
        this.localStartTime = localStartTime;
        this.localEndTime = localEndTime;
        this.status = status;
        this.totalPrice = totalPrice;
        this.currency = currency;
        this.holdExpiresAtUtcMillis = holdExpiresAtUtcMillis;
        this.cancellationReason = cancellationReason;
        this.cancelledAtUtcMillis = cancelledAtUtcMillis;
        this.createdAtUtcMillis = createdAtUtcMillis;
    }

    @NonNull
    public String getId() {
        return id;
    }

    @NonNull
    public String getCourtId() {
        return courtId;
    }

    @NonNull
    public String getCourtSportId() {
        return courtSportId;
    }

    @NonNull
    public Sport getSport() {
        return sport;
    }

    public long getStartsAtUtcMillis() {
        return startsAtUtcMillis;
    }

    public long getEndsAtUtcMillis() {
        return endsAtUtcMillis;
    }

    /** IANA time zone of the court. */
    @NonNull
    public String getTimeZoneId() {
        return timeZoneId;
    }

    /** Court-local date, {@code "yyyy-MM-dd"}. */
    @NonNull
    public String getLocalDate() {
        return localDate;
    }

    /** Court-local start, {@code "HH:mm"}. */
    @NonNull
    public String getLocalStartTime() {
        return localStartTime;
    }

    /** Court-local end, {@code "HH:mm"}. */
    @NonNull
    public String getLocalEndTime() {
        return localEndTime;
    }

    @NonNull
    public BookingStatus getStatus() {
        return status;
    }

    /** The backend's price snapshot; authoritative. */
    @NonNull
    public BigDecimal getTotalPrice() {
        return totalPrice;
    }

    @NonNull
    public String getCurrency() {
        return currency;
    }

    /** When the payment hold ends; only while Pending. */
    @Nullable
    public Long getHoldExpiresAtUtcMillis() {
        return holdExpiresAtUtcMillis;
    }

    @Nullable
    public String getCancellationReason() {
        return cancellationReason;
    }

    @Nullable
    public Long getCancelledAtUtcMillis() {
        return cancelledAtUtcMillis;
    }

    public long getCreatedAtUtcMillis() {
        return createdAtUtcMillis;
    }
}
