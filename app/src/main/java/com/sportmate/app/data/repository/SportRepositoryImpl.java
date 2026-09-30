package com.sportmate.app.data.repository;

import com.sportmate.app.data.remote.ApiCallExecutor;
import com.sportmate.app.data.remote.api.SportApi;
import com.sportmate.app.data.remote.mapper.DtoMapper;
import com.sportmate.app.domain.common.Cancellable;
import com.sportmate.app.domain.common.ResultCallback;
import com.sportmate.app.domain.model.Sport;
import com.sportmate.app.domain.repository.SportRepository;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public final class SportRepositoryImpl implements SportRepository {
    private final SportApi sportApi;
    private final ApiCallExecutor executor;

    @Inject
    public SportRepositoryImpl(SportApi sportApi, ApiCallExecutor executor) {
        this.sportApi = sportApi;
        this.executor = executor;
    }

    @Override
    public Cancellable getSports(ResultCallback<List<Sport>> callback) {
        return executor.execute(sportApi.getSports(), DtoMapper::toSports, callback);
    }
}
