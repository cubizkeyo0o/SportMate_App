package com.sportmate.app.domain.model;

import androidx.annotation.NonNull;

/**
 * One weekly opening period in the court's local wall-clock time. The times are kept exactly as
 * the backend sends them ("HH:mm") and are never converted to another time zone.
 */
public final class OperatingHour {
    private final Weekday day;
    private final String opensAt;
    private final String closesAt;

    public OperatingHour(@NonNull Weekday day, @NonNull String opensAt, @NonNull String closesAt) {
        this.day = day;
        this.opensAt = opensAt;
        this.closesAt = closesAt;
    }

    @NonNull
    public Weekday getDay() {
        return day;
    }

    @NonNull
    public String getOpensAt() {
        return opensAt;
    }

    @NonNull
    public String getClosesAt() {
        return closesAt;
    }
}
