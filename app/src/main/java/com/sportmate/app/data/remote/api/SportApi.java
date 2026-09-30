package com.sportmate.app.data.remote.api;

import com.sportmate.app.data.remote.dto.SportResponseDto;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;

public interface SportApi {
    @GET("api/v1/sports")
    Call<List<SportResponseDto>> getSports();
}
