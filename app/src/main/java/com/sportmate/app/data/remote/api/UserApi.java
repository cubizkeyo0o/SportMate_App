package com.sportmate.app.data.remote.api;

import com.sportmate.app.data.remote.dto.UserResponseDto;

import retrofit2.Call;
import retrofit2.http.GET;

public interface UserApi {
    @GET("api/v1/users/me")
    Call<UserResponseDto> getMe();
}
