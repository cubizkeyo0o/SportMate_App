package com.sportmate.app.data.remote;

import androidx.annotation.NonNull;

import com.sportmate.app.domain.common.AppError;
import com.sportmate.app.domain.common.Cancellable;
import com.sportmate.app.domain.common.ResultCallback;

import javax.inject.Inject;
import javax.inject.Singleton;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Runs Retrofit calls asynchronously, maps DTOs to domain models, and translates failures to
 * {@link AppError}. Callbacks arrive on Retrofit's callback executor (the main thread on Android)
 * and are dropped after {@link Cancellable#cancel()}.
 */
@Singleton
public final class ApiCallExecutor {
    /** Maps a successful response body to the result delivered to the caller. */
    public interface Mapper<D, T> {
        T map(@NonNull D body);
    }

    private final ApiErrorParser errorParser;

    @Inject
    public ApiCallExecutor(ApiErrorParser errorParser) {
        this.errorParser = errorParser;
    }

    /** For endpoints that return a body. A 2xx without a usable body is reported as UNKNOWN. */
    public <D, T> Cancellable execute(Call<D> call, Mapper<D, T> mapper, ResultCallback<T> callback) {
        call.enqueue(new Callback<D>() {
            @Override
            public void onResponse(@NonNull Call<D> call, @NonNull Response<D> response) {
                if (call.isCanceled()) {
                    return;
                }
                if (!response.isSuccessful()) {
                    callback.onError(errorParser.fromResponse(response));
                    return;
                }
                T result = mapOrNull(response.body(), mapper);
                if (result == null) {
                    callback.onError(AppError.of(AppError.Kind.UNKNOWN));
                } else {
                    callback.onSuccess(result);
                }
            }

            @Override
            public void onFailure(@NonNull Call<D> call, @NonNull Throwable throwable) {
                if (!call.isCanceled()) {
                    callback.onError(errorParser.fromThrowable(throwable));
                }
            }
        });
        return call::cancel;
    }

    /** For endpoints that answer 204 No Content. */
    public Cancellable executeNoContent(Call<Void> call, ResultCallback<Void> callback) {
        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                if (call.isCanceled()) {
                    return;
                }
                if (response.isSuccessful()) {
                    callback.onSuccess(null);
                } else {
                    callback.onError(errorParser.fromResponse(response));
                }
            }

            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable throwable) {
                if (!call.isCanceled()) {
                    callback.onError(errorParser.fromThrowable(throwable));
                }
            }
        });
        return call::cancel;
    }

    // A body missing required fields surfaces as a mapping exception; treat it as unreadable.
    private static <D, T> T mapOrNull(D body, Mapper<D, T> mapper) {
        if (body == null) {
            return null;
        }
        try {
            return mapper.map(body);
        } catch (RuntimeException e) {
            return null;
        }
    }
}
