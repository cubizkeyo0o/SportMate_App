package com.sportmate.app.data.repository;

import com.sportmate.app.data.remote.ApiCallExecutor;
import com.sportmate.app.data.remote.api.HealthApi;
import com.sportmate.app.domain.common.Cancellable;
import com.sportmate.app.domain.common.ResultCallback;
import com.sportmate.app.domain.repository.HealthRepository;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public final class HealthRepositoryImpl implements HealthRepository {
    private final HealthApi healthApi;
    private final ApiCallExecutor executor;

    @Inject
    public HealthRepositoryImpl(HealthApi healthApi, ApiCallExecutor executor) {
        this.healthApi = healthApi;
        this.executor = executor;
    }

    @Override
    public Cancellable checkHealth(ResultCallback<Boolean> callback) {
        return executor.execute(healthApi.getHealth(), body -> "ok".equals(body.status), callback);
    }
}
