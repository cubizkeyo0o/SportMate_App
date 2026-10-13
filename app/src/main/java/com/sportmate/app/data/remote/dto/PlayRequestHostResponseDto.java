package com.sportmate.app.data.remote.dto;

import com.google.gson.annotations.SerializedName;

public final class PlayRequestHostResponseDto {
    @SerializedName("display_name")
    public String displayName;
    @SerializedName("skill_level")
    public String skillLevel;
}
