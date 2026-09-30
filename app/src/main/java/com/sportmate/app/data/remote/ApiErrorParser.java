package com.sportmate.app.data.remote;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.sportmate.app.data.remote.dto.ProblemDetailsDto;
import com.sportmate.app.domain.common.AppError;

import java.io.IOException;

import javax.inject.Inject;
import javax.inject.Singleton;

import okhttp3.ResponseBody;
import retrofit2.Response;

/**
 * Converts failed Retrofit calls into {@link AppError}. Error bodies are read but never logged.
 */
@Singleton
public final class ApiErrorParser {
    private final Gson gson;

    @Inject
    public ApiErrorParser(Gson gson) {
        this.gson = gson;
    }

    @NonNull
    public AppError fromResponse(@NonNull Response<?> response) {
        int status = response.code();
        ProblemDetailsDto problem = readProblem(response.errorBody());
        return new AppError(
                kindOf(status),
                status,
                problem == null ? null : problem.title,
                problem == null ? null : problem.errors);
    }

    @NonNull
    public AppError fromThrowable(@NonNull Throwable throwable) {
        return throwable instanceof IOException
                ? AppError.of(AppError.Kind.NETWORK)
                : AppError.of(AppError.Kind.UNKNOWN);
    }

    @NonNull
    static AppError.Kind kindOf(int status) {
        switch (status) {
            case 400:
                return AppError.Kind.VALIDATION;
            case 401:
                return AppError.Kind.UNAUTHORIZED;
            case 403:
                return AppError.Kind.FORBIDDEN;
            case 404:
                return AppError.Kind.NOT_FOUND;
            case 409:
                return AppError.Kind.CONFLICT;
            case 422:
                return AppError.Kind.BUSINESS_RULE;
            default:
                return status >= 500 ? AppError.Kind.SERVER : AppError.Kind.UNKNOWN;
        }
    }

    // Bodies are not always ProblemDetails; the 401 JWT challenge, for example, is empty.
    @Nullable
    private ProblemDetailsDto readProblem(@Nullable ResponseBody body) {
        if (body == null) {
            return null;
        }
        try (ResponseBody closeable = body) {
            String json = closeable.string();
            return json.isEmpty() ? null : gson.fromJson(json, ProblemDetailsDto.class);
        } catch (IOException | JsonParseException | IllegalStateException e) {
            return null;
        }
    }
}
