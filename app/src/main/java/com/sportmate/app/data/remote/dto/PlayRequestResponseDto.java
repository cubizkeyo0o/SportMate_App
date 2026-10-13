package com.sportmate.app.data.remote.dto;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public final class PlayRequestResponseDto {
    @SerializedName("id")
    public String id;
    @SerializedName("sport_id")
    public String sportId;
    @SerializedName("sport_name")
    public String sportName;
    @SerializedName("sport_slug")
    public String sportSlug;
    @SerializedName("title")
    public String title;
    @SerializedName("description")
    public String description;
    @SerializedName("starts_at_utc")
    public String startsAtUtc;
    @SerializedName("ends_at_utc")
    public String endsAtUtc;
    @SerializedName("spots_needed")
    public Integer spotsNeeded;
    @SerializedName("accepted_count")
    public Integer acceptedCount;
    @SerializedName("location_name")
    public String locationName;
    @SerializedName("min_skill_level")
    public String minSkillLevel;
    @SerializedName("max_skill_level")
    public String maxSkillLevel;
    @SerializedName("status")
    public String status;
    @SerializedName("host")
    public PlayRequestHostResponseDto host;
    @SerializedName("my_participant_status")
    public String myParticipantStatus;
    @SerializedName("participants")
    public List<PlayRequestParticipantResponseDto> participants;
    @SerializedName("latitude")
    public Double latitude;
    @SerializedName("longitude")
    public Double longitude;
    @SerializedName("created_at_utc")
    public String createdAtUtc;
    @SerializedName("updated_at_utc")
    public String updatedAtUtc;
}
