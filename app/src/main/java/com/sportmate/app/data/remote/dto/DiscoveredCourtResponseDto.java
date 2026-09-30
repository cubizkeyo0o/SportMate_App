package com.sportmate.app.data.remote.dto;

import com.google.gson.annotations.SerializedName;

import java.math.BigDecimal;

/** Backend {@code DiscoveredCourtResponse}: a court as any signed-in user sees it; no management fields. */
public final class DiscoveredCourtResponseDto {
    @SerializedName("id")
    public String id;
    @SerializedName("name")
    public String name;
    @SerializedName("description")
    public String description;
    @SerializedName("address")
    public String address;
    @SerializedName("city")
    public String city;
    @SerializedName("latitude")
    public BigDecimal latitude;
    @SerializedName("longitude")
    public BigDecimal longitude;
    /** IANA time zone, e.g. {@code "Asia/Ho_Chi_Minh"}. */
    @SerializedName("time_zone_id")
    public String timeZoneId;
    /** Snake_case CourtStatus name; discovery only returns {@code "active"}. */
    @SerializedName("status")
    public String status;
}
