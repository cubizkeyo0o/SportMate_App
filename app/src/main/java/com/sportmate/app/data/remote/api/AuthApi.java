package com.sportmate.app.data.remote.api;

import com.sportmate.app.data.remote.dto.AuthResponseDto;
import com.sportmate.app.data.remote.dto.LoginRequestDto;
import com.sportmate.app.data.remote.dto.RefreshTokenRequestDto;
import com.sportmate.app.data.remote.dto.RegisterRequestDto;
import com.sportmate.app.data.remote.dto.UserResponseDto;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.POST;

public interface AuthApi {
    /** 201 with the created user; the caller is not signed in. */
    @POST("api/v1/auth/register")
    Call<UserResponseDto> register(@Body RegisterRequestDto request);

    @POST("api/v1/auth/login")
    Call<AuthResponseDto> login(@Body LoginRequestDto request);

    /**
     * Requires the access token, passed explicitly because local tokens are cleared before the call.
     * 204 on success.
     */
    @POST("api/v1/auth/logout")
    Call<Void> logout(
            @Header("Authorization") String authorization,
            @Body RefreshTokenRequestDto request);
}
