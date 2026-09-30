package com.sportmate.app.data.remote.api;

import com.sportmate.app.data.remote.dto.SetSportProfileRequestDto;
import com.sportmate.app.data.remote.dto.UpdateProfileRequestDto;
import com.sportmate.app.data.remote.dto.UserResponseDto;
import com.sportmate.app.data.remote.dto.UserSportProfileResponseDto;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.PUT;
import retrofit2.http.Path;

/** The authenticated user's own resources; the user always comes from the access token. */
public interface UserApi {
    @GET("api/v1/users/me")
    Call<UserResponseDto> getMe();

    @PUT("api/v1/users/me")
    Call<UserResponseDto> updateMe(@Body UpdateProfileRequestDto request);

    /** Ordered by sport name. */
    @GET("api/v1/users/me/sports")
    Call<List<UserSportProfileResponseDto>> getMySports();

    /** Creates the profile or changes its skill level; 200 either way. */
    @PUT("api/v1/users/me/sports/{sportId}")
    Call<UserSportProfileResponseDto> setMySport(
            @Path("sportId") String sportId,
            @Body SetSportProfileRequestDto request);

    /** 204, also when there was no such profile. */
    @DELETE("api/v1/users/me/sports/{sportId}")
    Call<Void> removeMySport(@Path("sportId") String sportId);
}
