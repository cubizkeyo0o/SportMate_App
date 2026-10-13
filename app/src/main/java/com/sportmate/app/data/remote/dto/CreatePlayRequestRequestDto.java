package com.sportmate.app.data.remote.dto;

import com.google.gson.annotations.SerializedName;

public final class CreatePlayRequestRequestDto {
    @SerializedName("sport_id")
    public final String sportId;
    @SerializedName("title")
    public final String title;
    @SerializedName("description")
    public final String description;
    @SerializedName("starts_at")
    public final String startsAt;
    @SerializedName("ends_at")
    public final String endsAt;
    @SerializedName("spots_needed")
    public final int spotsNeeded;
    @SerializedName("latitude")
    public final double latitude;
    @SerializedName("longitude")
    public final double longitude;
    @SerializedName("min_skill_level")
    public final String minSkillLevel;
    @SerializedName("max_skill_level")
    public final String maxSkillLevel;
    @SerializedName("location_name")
    public final String locationName;

    public CreatePlayRequestRequestDto(
            String sportId,
            String title,
            String description,
            String startsAt,
            String endsAt,
            int spotsNeeded,
            double latitude,
            double longitude,
            String minSkillLevel,
            String maxSkillLevel,
            String locationName) {
        this.sportId = sportId;
        this.title = title;
        this.description = description;
        this.startsAt = startsAt;
        this.endsAt = endsAt;
        this.spotsNeeded = spotsNeeded;
        this.latitude = latitude;
        this.longitude = longitude;
        this.minSkillLevel = minSkillLevel;
        this.maxSkillLevel = maxSkillLevel;
        this.locationName = locationName;
    }
}
