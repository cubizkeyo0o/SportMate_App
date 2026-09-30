package com.sportmate.app.data.remote.dto;

import com.google.gson.annotations.SerializedName;

/** Backend {@code SetSportProfileRequest}. */
public final class SetSportProfileRequestDto {
    /** Snake_case SkillLevel name, e.g. {@code "beginner"}. */
    @SerializedName("skill_level")
    public final String skillLevel;

    public SetSportProfileRequestDto(String skillLevel) {
        this.skillLevel = skillLevel;
    }
}
