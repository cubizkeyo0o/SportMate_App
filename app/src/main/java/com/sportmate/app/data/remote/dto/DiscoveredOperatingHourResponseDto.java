package com.sportmate.app.data.remote.dto;

import com.google.gson.annotations.SerializedName;

/** Backend {@code DiscoveredOperatingHourResponse}. Times are court-local wall-clock times. */
public final class DiscoveredOperatingHourResponseDto {
    /** 0 = Sunday ... 6 = Saturday. */
    @SerializedName("day_of_week")
    public Integer dayOfWeek;
    /** {@code "HH:mm"}. */
    @SerializedName("opens_at")
    public String opensAt;
    /** {@code "HH:mm"}. */
    @SerializedName("closes_at")
    public String closesAt;
}
