package com.sportmate.app.domain.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class PlayRequest {
    private final String id;
    private final String sportId;
    private final String sportName;
    private final String sportSlug;
    private final String title;
    @Nullable
    private final String description;
    private final long startsAtUtcMillis;
    private final long endsAtUtcMillis;
    private final int spotsNeeded;
    private final int acceptedCount;
    @Nullable
    private final String locationName;
    @Nullable
    private final SkillLevel minSkillLevel;
    @Nullable
    private final SkillLevel maxSkillLevel;
    private final PlayRequestStatus status;
    private final String hostDisplayName;
    @Nullable
    private final SkillLevel hostSkillLevel;
    @Nullable
    private final ParticipantStatus myParticipantStatus;
    private final List<PlayRequestParticipant> participants;
    @Nullable
    private final Double latitude;
    @Nullable
    private final Double longitude;
    private final long createdAtUtcMillis;
    private final long updatedAtUtcMillis;

    public PlayRequest(
            @NonNull String id,
            @NonNull String sportId,
            @NonNull String sportName,
            @NonNull String sportSlug,
            @NonNull String title,
            @Nullable String description,
            long startsAtUtcMillis,
            long endsAtUtcMillis,
            int spotsNeeded,
            int acceptedCount,
            @Nullable String locationName,
            @Nullable SkillLevel minSkillLevel,
            @Nullable SkillLevel maxSkillLevel,
            @NonNull PlayRequestStatus status,
            @NonNull String hostDisplayName,
            @Nullable SkillLevel hostSkillLevel,
            @Nullable ParticipantStatus myParticipantStatus,
            @Nullable List<PlayRequestParticipant> participants,
            @Nullable Double latitude,
            @Nullable Double longitude,
            long createdAtUtcMillis,
            long updatedAtUtcMillis) {
        this.id = id;
        this.sportId = sportId;
        this.sportName = sportName;
        this.sportSlug = sportSlug;
        this.title = title;
        this.description = description;
        this.startsAtUtcMillis = startsAtUtcMillis;
        this.endsAtUtcMillis = endsAtUtcMillis;
        this.spotsNeeded = spotsNeeded;
        this.acceptedCount = acceptedCount;
        this.locationName = locationName;
        this.minSkillLevel = minSkillLevel;
        this.maxSkillLevel = maxSkillLevel;
        this.status = status;
        this.hostDisplayName = hostDisplayName;
        this.hostSkillLevel = hostSkillLevel;
        this.myParticipantStatus = myParticipantStatus;
        this.participants = Collections.unmodifiableList(new ArrayList<>(
                participants == null ? Collections.emptyList() : participants));
        this.latitude = latitude;
        this.longitude = longitude;
        this.createdAtUtcMillis = createdAtUtcMillis;
        this.updatedAtUtcMillis = updatedAtUtcMillis;
    }

    @NonNull public String getId() { return id; }
    @NonNull public String getSportId() { return sportId; }
    @NonNull public String getSportName() { return sportName; }
    @NonNull public String getSportSlug() { return sportSlug; }
    @NonNull public String getTitle() { return title; }
    @Nullable public String getDescription() { return description; }
    public long getStartsAtUtcMillis() { return startsAtUtcMillis; }
    public long getEndsAtUtcMillis() { return endsAtUtcMillis; }
    public int getSpotsNeeded() { return spotsNeeded; }
    public int getAcceptedCount() { return acceptedCount; }
    @Nullable public String getLocationName() { return locationName; }
    @Nullable public SkillLevel getMinSkillLevel() { return minSkillLevel; }
    @Nullable public SkillLevel getMaxSkillLevel() { return maxSkillLevel; }
    @NonNull public PlayRequestStatus getStatus() { return status; }
    @NonNull public String getHostDisplayName() { return hostDisplayName; }
    @Nullable public SkillLevel getHostSkillLevel() { return hostSkillLevel; }
    @Nullable public ParticipantStatus getMyParticipantStatus() { return myParticipantStatus; }
    @NonNull public List<PlayRequestParticipant> getParticipants() { return participants; }
    @Nullable public Double getLatitude() { return latitude; }
    @Nullable public Double getLongitude() { return longitude; }
    public long getCreatedAtUtcMillis() { return createdAtUtcMillis; }
    public long getUpdatedAtUtcMillis() { return updatedAtUtcMillis; }

    public boolean isHost() {
        return latitude != null && longitude != null;
    }
}
