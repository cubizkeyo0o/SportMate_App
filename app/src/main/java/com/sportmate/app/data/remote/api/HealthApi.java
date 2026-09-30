package com.sportmate.app.data.remote.api;

import com.sportmate.app.data.remote.dto.HealthResponseDto;

import retrofit2.Call;
import retrofit2.http.GET;

public interface HealthApi {
    @GET("api/v1/health")
    Call<HealthResponseDto> getHealth();
}
