package com.sportmate.app.data.repository;

import com.sportmate.app.data.api.ApiService;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public final class HealthRepository {
    private final ApiService apiService;

    @Inject
    public HealthRepository(ApiService apiService) {
        this.apiService = apiService;
    }

    public ApiService getApiService() {
        return apiService;
    }
}