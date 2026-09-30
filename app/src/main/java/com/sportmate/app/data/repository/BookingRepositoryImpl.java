package com.sportmate.app.data.repository;

import com.sportmate.app.data.remote.ApiCallExecutor;
import com.sportmate.app.data.remote.UtcTimestamps;
import com.sportmate.app.data.remote.api.BookingApi;
import com.sportmate.app.data.remote.dto.CreateBookingRequestDto;
import com.sportmate.app.data.remote.mapper.DtoMapper;
import com.sportmate.app.domain.common.Cancellable;
import com.sportmate.app.domain.common.ResultCallback;
import com.sportmate.app.domain.model.Booking;
import com.sportmate.app.domain.repository.BookingRepository;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public final class BookingRepositoryImpl implements BookingRepository {
    private final BookingApi api;
    private final ApiCallExecutor executor;

    @Inject
    public BookingRepositoryImpl(BookingApi api, ApiCallExecutor executor) {
        this.api = api;
        this.executor = executor;
    }

    @Override
    public Cancellable createBooking(
            String courtSportId,
            long startsAtUtcMillis,
            long endsAtUtcMillis,
            ResultCallback<Booking> callback) {
        CreateBookingRequestDto request = new CreateBookingRequestDto(
                courtSportId,
                UtcTimestamps.format(startsAtUtcMillis),
                UtcTimestamps.format(endsAtUtcMillis));
        return executor.execute(api.create(request), DtoMapper::toBooking, callback);
    }

    @Override
    public Cancellable getBooking(String bookingId, ResultCallback<Booking> callback) {
        return executor.execute(api.get(bookingId), DtoMapper::toBooking, callback);
    }

    @Override
    public Cancellable getMyBookings(ResultCallback<List<Booking>> callback) {
        return executor.execute(api.getMine(), DtoMapper::toBookings, callback);
    }

    @Override
    public Cancellable pay(String bookingId, ResultCallback<Booking> callback) {
        return executor.execute(api.pay(bookingId), DtoMapper::toBooking, callback);
    }

    @Override
    public Cancellable cancel(String bookingId, ResultCallback<Booking> callback) {
        return executor.execute(api.cancel(bookingId), DtoMapper::toBooking, callback);
    }
}
