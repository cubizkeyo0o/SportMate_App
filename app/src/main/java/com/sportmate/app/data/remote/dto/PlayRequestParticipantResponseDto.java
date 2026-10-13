package com.sportmate.app.data.remote.dto;

import com.google.gson.annotations.SerializedName;

public final class PlayRequestParticipantResponseDto {
    @SerializedName("id")
    public String id;
    @SerializedName("display_name")
    public String displayName;
    @SerializedName("skill_level")
    public String skillLevel;
    @SerializedName("status")
    public String status;
    @SerializedName("requested_at_utc")
    public String requestedAtUtc;
}
