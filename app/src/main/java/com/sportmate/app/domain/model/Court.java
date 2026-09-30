package com.sportmate.app.domain.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.math.BigDecimal;

/** A discoverable court: one physical court that may offer several sports. */
public final class Court {
    private final String id;
    private final String name;
    @Nullable
    private final String description;
    private final String address;
    private final String city;
    private final BigDecimal latitude;
    private final BigDecimal longitude;
    private final String timeZoneId;
    private final CourtStatus status;

    public Court(
            @NonNull String id,
            @NonNull String name,
            @Nullable String description,
            @NonNull String address,
            @NonNull String city,
            @NonNull BigDecimal latitude,
            @NonNull BigDecimal longitude,
            @NonNull String timeZoneId,
            @NonNull CourtStatus status) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.address = address;
        this.city = city;
        this.latitude = latitude;
        this.longitude = longitude;
        this.timeZoneId = timeZoneId;
        this.status = status;
    }

    @NonNull
    public String getId() {
        return id;
    }

    @NonNull
    public String getName() {
        return name;
    }

    @Nullable
    public String getDescription() {
        return description;
    }

    @NonNull
    public String getAddress() {
        return address;
    }

    @NonNull
    public String getCity() {
        return city;
    }

    @NonNull
    public BigDecimal getLatitude() {
        return latitude;
    }

    @NonNull
    public BigDecimal getLongitude() {
        return longitude;
    }

    /** IANA time zone the court's operating hours are expressed in. */
    @NonNull
    public String getTimeZoneId() {
        return timeZoneId;
    }

    @NonNull
    public CourtStatus getStatus() {
        return status;
    }
}
