package com.sportmate.app.presentation.booking;

import androidx.annotation.Nullable;

/**
 * What the user picked on the booking screen. Date and times are the court's local wall-clock
 * values ({@code "yyyy-MM-dd"}, {@code "HH:mm"}); the court-sport id comes from court discovery.
 */
public final class BookingSelection {
    @Nullable
    final String courtSportId;
    @Nullable
    final String date;
    @Nullable
    final String startTime;
    @Nullable
    final String endTime;

    BookingSelection(
            @Nullable String courtSportId,
            @Nullable String date,
            @Nullable String startTime,
            @Nullable String endTime) {
        this.courtSportId = courtSportId;
        this.date = date;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    static BookingSelection empty() {
        return new BookingSelection(null, null, null, null);
    }

    BookingSelection withCourtSport(String id) {
        return new BookingSelection(id, date, startTime, endTime);
    }

    BookingSelection withDate(String value) {
        return new BookingSelection(courtSportId, value, startTime, endTime);
    }

    BookingSelection withStartTime(String value) {
        return new BookingSelection(courtSportId, date, value, endTime);
    }

    BookingSelection withEndTime(String value) {
        return new BookingSelection(courtSportId, date, startTime, value);
    }

    @Nullable
    public String getCourtSportId() {
        return courtSportId;
    }

    @Nullable
    public String getDate() {
        return date;
    }

    @Nullable
    public String getStartTime() {
        return startTime;
    }

    @Nullable
    public String getEndTime() {
        return endTime;
    }
}
