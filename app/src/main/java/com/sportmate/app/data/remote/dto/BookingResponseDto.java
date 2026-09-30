package com.sportmate.app.data.remote.dto;

import com.google.gson.annotations.SerializedName;

import java.math.BigDecimal;

/**
 * Backend {@code BookingResponse}. {@code *_utc} fields are UTC instants; {@code local_*} fields are
 * the same start and end as wall-clock time of the court ({@code time_zone_id}).
 */
public final class BookingResponseDto {
    @SerializedName("id")
    public String id;
    @SerializedName("user_id")
    public String userId;
    @SerializedName("court_id")
    public String courtId;
    @SerializedName("court_sport_id")
    public String courtSportId;
    @SerializedName("sport")
    public SportSummaryResponseDto sport;
    @SerializedName("starts_at_utc")
    public String startsAtUtc;
    @SerializedName("ends_at_utc")
    public String endsAtUtc;
    @SerializedName("time_zone_id")
    public String timeZoneId;
    /** {@code "yyyy-MM-dd"}, court-local. */
    @SerializedName("local_date")
    public String localDate;
    /** {@code "HH:mm"}, court-local. */
    @SerializedName("local_start_time")
    public String localStartTime;
    /** {@code "HH:mm"}, court-local. */
    @SerializedName("local_end_time")
    public String localEndTime;
    /** Snake_case BookingStatus name, e.g. {@code "pending"}, {@code "no_show"}. */
    @SerializedName("status")
    public String status;
    /** Price snapshot calculated by the backend when the booking was created. */
    @SerializedName("total_price")
    public BigDecimal totalPrice;
    @SerializedName("currency")
    public String currency;
    /** Only while Pending. */
    @SerializedName("hold_expires_at_utc")
    public String holdExpiresAtUtc;
    @SerializedName("cancellation_reason")
    public String cancellationReason;
    @SerializedName("cancelled_at_utc")
    public String cancelledAtUtc;
    @SerializedName("created_at_utc")
    public String createdAtUtc;
    @SerializedName("updated_at_utc")
    public String updatedAtUtc;
}
