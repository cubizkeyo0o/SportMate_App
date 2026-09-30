package com.sportmate.app.data.remote.dto;

import com.google.gson.annotations.SerializedName;

/** Backend {@code UserSportProfileResponse}. */
public final class UserSportProfileResponseDto {
    @SerializedName("sport")
    public SportSummaryResponseDto sport;
    /** Snake_case SkillLevel name, e.g. {@code "beginner"}. */
    @SerializedName("skill_level")
    public String skillLevel;
}
