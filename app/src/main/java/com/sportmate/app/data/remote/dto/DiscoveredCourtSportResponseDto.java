package com.sportmate.app.data.remote.dto;

import com.google.gson.annotations.SerializedName;

import java.math.BigDecimal;

/** Backend {@code DiscoveredCourtSportResponse}: a sport the court offers now, with its hourly price. */
public final class DiscoveredCourtSportResponseDto {
    /** The court-sport link id, sent as {@code court_sport_id} when creating a booking. */
    @SerializedName("court_sport_id")
    public String courtSportId;
    @SerializedName("sport")
    public SportResponseDto sport;
    /** Decimal, parsed exactly. */
    @SerializedName("price_per_hour")
    public BigDecimal pricePerHour;
    /** ISO 4217 code, uppercase. */
    @SerializedName("currency")
    public String currency;
    /** Always true in discovery: only offered sports are returned. */
    @SerializedName("is_active")
    public Boolean isActive;
}
