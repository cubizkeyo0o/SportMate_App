package com.sportmate.app.data.remote.dto;

import com.google.gson.annotations.SerializedName;

/**
 * Backend {@code CreateBookingRequest}. The court, price and status are decided by the backend.
 * Times are ISO-8601 instants with {@code Z} or an explicit offset, in whole minutes.
 */
public final class CreateBookingRequestDto {
    /** The court-sport link id from court discovery, not the sport's id. */
    @SerializedName("court_sport_id")
    public final String courtSportId;
    @SerializedName("starts_at")
    public final String startsAt;
    @SerializedName("ends_at")
    public final String endsAt;

    public CreateBookingRequestDto(String courtSportId, String startsAt, String endsAt) {
        this.courtSportId = courtSportId;
        this.startsAt = startsAt;
        this.endsAt = endsAt;
    }
}
