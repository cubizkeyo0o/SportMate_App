package com.sportmate.app.domain.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public final class PlayRequestParticipant {
    private final String id;
    private final String displayName;
    @Nullable
    private final SkillLevel skillLevel;
    private final ParticipantStatus status;
    private final long requestedAtUtcMillis;

    public PlayRequestParticipant(
            @NonNull String id,
            @NonNull String displayName,
            @Nullable SkillLevel skillLevel,
            @NonNull ParticipantStatus status,
            long requestedAtUtcMillis) {
        this.id = id;
        this.displayName = displayName;
        this.skillLevel = skillLevel;
        this.status = status;
        this.requestedAtUtcMillis = requestedAtUtcMillis;
    }

    @NonNull
    public String getId() {
        return id;
    }

    @NonNull
    public String getDisplayName() {
        return displayName;
    }

    @Nullable
    public SkillLevel getSkillLevel() {
        return skillLevel;
    }

    @NonNull
    public ParticipantStatus getStatus() {
        return status;
    }

    public long getRequestedAtUtcMillis() {
        return requestedAtUtcMillis;
    }
}
