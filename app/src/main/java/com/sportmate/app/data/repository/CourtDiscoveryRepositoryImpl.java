package com.sportmate.app.data.repository;

import com.sportmate.app.data.remote.ApiCallExecutor;
import com.sportmate.app.data.remote.api.CourtDiscoveryApi;
import com.sportmate.app.data.remote.mapper.DtoMapper;
import com.sportmate.app.domain.common.Cancellable;
import com.sportmate.app.domain.common.ResultCallback;
import com.sportmate.app.domain.model.Court;
import com.sportmate.app.domain.model.CourtSport;
import com.sportmate.app.domain.model.OperatingHour;
import com.sportmate.app.domain.repository.CourtDiscoveryRepository;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public final class CourtDiscoveryRepositoryImpl implements CourtDiscoveryRepository {
    private final CourtDiscoveryApi api;
    private final ApiCallExecutor executor;

    @Inject
    public CourtDiscoveryRepositoryImpl(CourtDiscoveryApi api, ApiCallExecutor executor) {
        this.api = api;
        this.executor = executor;
    }

    @Override
    public Cancellable getCourts(ResultCallback<List<Court>> callback) {
        return executor.execute(api.getCourts(), DtoMapper::toCourts, callback);
    }

    @Override
    public Cancellable getCourt(String courtId, ResultCallback<Court> callback) {
        return executor.execute(api.getCourt(courtId), DtoMapper::toCourt, callback);
    }

    @Override
    public Cancellable getCourtSports(String courtId, ResultCallback<List<CourtSport>> callback) {
        return executor.execute(api.getCourtSports(courtId), DtoMapper::toCourtSports, callback);
    }

    @Override
    public Cancellable getOperatingHours(String courtId, ResultCallback<List<OperatingHour>> callback) {
        return executor.execute(api.getOperatingHours(courtId), DtoMapper::toOperatingHours, callback);
    }
}
