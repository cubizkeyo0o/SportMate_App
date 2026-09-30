package com.sportmate.app.data.remote.api;

import com.sportmate.app.data.remote.dto.BookingResponseDto;
import com.sportmate.app.data.remote.dto.CreateBookingRequestDto;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;

/** The signed-in user's bookings. Bookings the caller may not see are 404. */
public interface BookingApi {
    /** 201 with a Pending booking that holds the court until {@code hold_expires_at_utc}. */
    @POST("api/v1/bookings")
    Call<BookingResponseDto> create(@Body CreateBookingRequestDto request);

    /** The caller's own bookings, ordered by start time. */
    @GET("api/v1/bookings/me")
    Call<List<BookingResponseDto>> getMine();

    @GET("api/v1/bookings/{id}")
    Call<BookingResponseDto> get(@Path("id") String id);

    /** Mock payment, no body: Pending to Confirmed while the hold runs. Booker only. */
    @POST("api/v1/bookings/{id}/pay")
    Call<BookingResponseDto> pay(@Path("id") String id);

    /** No body (the reason is optional): Pending or Confirmed to Cancelled before the start. */
    @POST("api/v1/bookings/{id}/cancel")
    Call<BookingResponseDto> cancel(@Path("id") String id);
}
