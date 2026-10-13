package com.sportmate.app.domain.repository;

import androidx.annotation.Nullable;

import com.sportmate.app.domain.common.Cancellable;
import com.sportmate.app.domain.common.ResultCallback;
import com.sportmate.app.domain.model.PlayRequest;
import com.sportmate.app.domain.model.PlayRequestPage;
import com.sportmate.app.domain.model.PlayRequestParticipant;

import java.util.List;

public interface PlayRequestRepository {
    Cancellable discover(
            @Nullable String sportId,
            @Nullable String startsFromUtc,
            @Nullable String startsToUtc,
            @Nullable Double latitude,
            @Nullable Double longitude,
            @Nullable Double radiusKm,
            int page,
            int pageSize,
            ResultCallback<PlayRequestPage> callback);

    Cancellable create(
            String sportId,
            String title,
            @Nullable String description,
            long startsAtUtcMillis,
            long endsAtUtcMillis,
            int spotsNeeded,
            double latitude,
            double longitude,
            @Nullable String minSkillLevel,
            @Nullable String maxSkillLevel,
            @Nullable String locationName,
            ResultCallback<PlayRequest> callback);

    Cancellable get(String id, ResultCallback<PlayRequest> callback);
    Cancellable getMine(int page, int pageSize, ResultCallback<PlayRequestPage> callback);
    Cancellable getMyParticipations(int page, int pageSize, ResultCallback<PlayRequestPage> callback);
    Cancellable getParticipants(String id, ResultCallback<List<PlayRequestParticipant>> callback);
    Cancellable join(String id, ResultCallback<PlayRequest> callback);
    Cancellable withdraw(String id, ResultCallback<PlayRequest> callback);
    Cancellable accept(String id, String participantId, ResultCallback<PlayRequest> callback);
    Cancellable reject(String id, String participantId, ResultCallback<PlayRequest> callback);
    Cancellable cancel(String id, ResultCallback<PlayRequest> callback);
    Cancellable complete(String id, ResultCallback<PlayRequest> callback);
}
