package com.sportmate.app.data.remote.api;

import com.sportmate.app.data.remote.dto.DiscoveredCourtResponseDto;
import com.sportmate.app.data.remote.dto.DiscoveredCourtSportResponseDto;
import com.sportmate.app.data.remote.dto.DiscoveredOperatingHourResponseDto;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

/**
 * Read-only court browsing for any signed-in user. Only active courts are visible; an inactive or
 * unknown court is 404 on every detail route.
 */
public interface CourtDiscoveryApi {
    /** Active courts, ordered by name. */
    @GET("api/v1/discovery/courts")
    Call<List<DiscoveredCourtResponseDto>> getCourts();

    @GET("api/v1/discovery/courts/{courtId}")
    Call<DiscoveredCourtResponseDto> getCourt(@Path("courtId") String courtId);

    /** Sports offered now, ordered by sport name. */
    @GET("api/v1/discovery/courts/{courtId}/sports")
    Call<List<DiscoveredCourtSportResponseDto>> getCourtSports(@Path("courtId") String courtId);

    /** Weekly periods, ordered by day then opening time. */
    @GET("api/v1/discovery/courts/{courtId}/operating-hours")
    Call<List<DiscoveredOperatingHourResponseDto>> getOperatingHours(@Path("courtId") String courtId);
}
