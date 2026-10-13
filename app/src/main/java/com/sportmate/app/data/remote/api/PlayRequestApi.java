package com.sportmate.app.data.remote.api;

import com.sportmate.app.data.remote.dto.CreatePlayRequestRequestDto;
import com.sportmate.app.data.remote.dto.PlayRequestPageResponseDto;
import com.sportmate.app.data.remote.dto.PlayRequestParticipantResponseDto;
import com.sportmate.app.data.remote.dto.PlayRequestResponseDto;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface PlayRequestApi {
    @GET("api/v1/play-requests")
    Call<PlayRequestPageResponseDto> discover(
            @Query("sport_id") String sportId,
            @Query("starts_from_utc") String startsFromUtc,
            @Query("starts_to_utc") String startsToUtc,
            @Query("latitude") Double latitude,
            @Query("longitude") Double longitude,
            @Query("radius_km") Double radiusKm,
            @Query("page") int page,
            @Query("page_size") int pageSize);

    @POST("api/v1/play-requests")
    Call<PlayRequestResponseDto> create(@Body CreatePlayRequestRequestDto request);

    @GET("api/v1/play-requests/{id}")
    Call<PlayRequestResponseDto> get(@Path("id") String id);

    @GET("api/v1/play-requests/me")
    Call<PlayRequestPageResponseDto> getMine(
            @Query("page") int page,
            @Query("page_size") int pageSize);

    @GET("api/v1/play-requests/me/participations")
    Call<PlayRequestPageResponseDto> getMyParticipations(
            @Query("page") int page,
            @Query("page_size") int pageSize);

    @GET("api/v1/play-requests/{id}/participants")
    Call<List<PlayRequestParticipantResponseDto>> getParticipants(@Path("id") String id);

    @POST("api/v1/play-requests/{id}/participants")
    Call<PlayRequestResponseDto> join(@Path("id") String id);

    @POST("api/v1/play-requests/{id}/withdraw")
    Call<PlayRequestResponseDto> withdraw(@Path("id") String id);

    @POST("api/v1/play-requests/{id}/participants/{participantId}/accept")
    Call<PlayRequestResponseDto> accept(
            @Path("id") String id,
            @Path("participantId") String participantId);

    @POST("api/v1/play-requests/{id}/participants/{participantId}/reject")
    Call<PlayRequestResponseDto> reject(
            @Path("id") String id,
            @Path("participantId") String participantId);

    @POST("api/v1/play-requests/{id}/cancel")
    Call<PlayRequestResponseDto> cancel(@Path("id") String id);

    @POST("api/v1/play-requests/{id}/complete")
    Call<PlayRequestResponseDto> complete(@Path("id") String id);
}
