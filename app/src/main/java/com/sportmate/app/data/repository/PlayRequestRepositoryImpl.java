package com.sportmate.app.data.repository;

import androidx.annotation.Nullable;

import com.sportmate.app.data.remote.ApiCallExecutor;
import com.sportmate.app.data.remote.UtcTimestamps;
import com.sportmate.app.data.remote.api.PlayRequestApi;
import com.sportmate.app.data.remote.dto.CreatePlayRequestRequestDto;
import com.sportmate.app.data.remote.mapper.DtoMapper;
import com.sportmate.app.domain.common.Cancellable;
import com.sportmate.app.domain.common.ResultCallback;
import com.sportmate.app.domain.model.PlayRequest;
import com.sportmate.app.domain.model.PlayRequestPage;
import com.sportmate.app.domain.model.PlayRequestParticipant;
import com.sportmate.app.domain.repository.PlayRequestRepository;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public final class PlayRequestRepositoryImpl implements PlayRequestRepository {
    private final PlayRequestApi api;
    private final ApiCallExecutor executor;

    @Inject
    public PlayRequestRepositoryImpl(PlayRequestApi api, ApiCallExecutor executor) {
        this.api = api;
        this.executor = executor;
    }

    @Override
    public Cancellable discover(
            @Nullable String sportId,
            @Nullable String startsFromUtc,
            @Nullable String startsToUtc,
            @Nullable Double latitude,
            @Nullable Double longitude,
            @Nullable Double radiusKm,
            int page,
            int pageSize,
            ResultCallback<PlayRequestPage> callback) {
        return executor.execute(
                api.discover(sportId, startsFromUtc, startsToUtc, latitude, longitude, radiusKm, page, pageSize),
                DtoMapper::toPlayRequestPage,
                callback);
    }

    @Override
    public Cancellable create(
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
            ResultCallback<PlayRequest> callback) {
        CreatePlayRequestRequestDto request = new CreatePlayRequestRequestDto(
                sportId, title, description, UtcTimestamps.format(startsAtUtcMillis),
                UtcTimestamps.format(endsAtUtcMillis), spotsNeeded, latitude, longitude,
                minSkillLevel, maxSkillLevel, locationName);
        return executor.execute(api.create(request), DtoMapper::toPlayRequest, callback);
    }

    @Override
    public Cancellable get(String id, ResultCallback<PlayRequest> callback) {
        return executor.execute(api.get(id), DtoMapper::toPlayRequest, callback);
    }

    @Override
    public Cancellable getMine(int page, int pageSize, ResultCallback<PlayRequestPage> callback) {
        return executor.execute(api.getMine(page, pageSize), DtoMapper::toPlayRequestPage, callback);
    }

    @Override
    public Cancellable getMyParticipations(int page, int pageSize, ResultCallback<PlayRequestPage> callback) {
        return executor.execute(api.getMyParticipations(page, pageSize), DtoMapper::toPlayRequestPage, callback);
    }

    @Override
    public Cancellable getParticipants(String id, ResultCallback<List<PlayRequestParticipant>> callback) {
        return executor.execute(api.getParticipants(id), DtoMapper::toPlayRequestParticipants, callback);
    }

    @Override
    public Cancellable join(String id, ResultCallback<PlayRequest> callback) {
        return executor.execute(api.join(id), DtoMapper::toPlayRequest, callback);
    }

    @Override
    public Cancellable withdraw(String id, ResultCallback<PlayRequest> callback) {
        return executor.execute(api.withdraw(id), DtoMapper::toPlayRequest, callback);
    }

    @Override
    public Cancellable accept(String id, String participantId, ResultCallback<PlayRequest> callback) {
        return executor.execute(api.accept(id, participantId), DtoMapper::toPlayRequest, callback);
    }

    @Override
    public Cancellable reject(String id, String participantId, ResultCallback<PlayRequest> callback) {
        return executor.execute(api.reject(id, participantId), DtoMapper::toPlayRequest, callback);
    }

    @Override
    public Cancellable cancel(String id, ResultCallback<PlayRequest> callback) {
        return executor.execute(api.cancel(id), DtoMapper::toPlayRequest, callback);
    }

    @Override
    public Cancellable complete(String id, ResultCallback<PlayRequest> callback) {
        return executor.execute(api.complete(id), DtoMapper::toPlayRequest, callback);
    }
}
