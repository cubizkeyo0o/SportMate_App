package com.sportmate.app.data.api;

import com.sportmate.app.data.model.HealthResponse;

import retrofit2.Call;
import retrofit2.http.GET;

public interface ApiService {
    @GET("api/v1/health")
    Call<HealthResponse> getHealth();
}