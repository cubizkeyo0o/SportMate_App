package com.sportmate.app.domain.model;

import androidx.annotation.NonNull;

/**
 * The user's skill level in one sport. The sport may have been deactivated after the profile was
 * created; such profiles can still be edited and removed, but not created.
 */
public final class UserSportProfile {
    private final Sport sport;
    private final boolean sportActive;
    private final SkillLevel skillLevel;

    public UserSportProfile(@NonNull Sport sport, boolean sportActive, @NonNull SkillLevel skillLevel) {
        this.sport = sport;
        this.sportActive = sportActive;
        this.skillLevel = skillLevel;
    }

    @NonNull
    public Sport getSport() {
        return sport;
    }

    public boolean isSportActive() {
        return sportActive;
    }

    @NonNull
    public SkillLevel getSkillLevel() {
        return skillLevel;
    }
}
