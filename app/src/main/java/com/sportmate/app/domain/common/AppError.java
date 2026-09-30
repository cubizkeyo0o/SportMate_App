package com.sportmate.app.domain.common;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * A failed operation, independent of Retrofit/OkHttp. The UI decides how to present it.
 */
public final class AppError {
    public enum Kind {
        /** No response: offline, timeout, host unreachable. */
        NETWORK,
        /** 400: invalid input; see {@link #getFieldErrors()}. */
        VALIDATION,
        /** 401: not signed in or session no longer valid. */
        UNAUTHORIZED,
        /** 403: signed in but not allowed. */
        FORBIDDEN,
        /** 404. */
        NOT_FOUND,
        /** 409: conflicts with existing state (duplicate, booking overlap, ...). */
        CONFLICT,
        /** 422: a domain rule rejected the request. */
        BUSINESS_RULE,
        /** 5xx. */
        SERVER,
        /** Anything else, including unreadable responses. */
        UNKNOWN
    }

    private final Kind kind;
    private final int httpStatus;
    @Nullable
    private final String message;
    private final Map<String, List<String>> fieldErrors;

    public AppError(
            @NonNull Kind kind,
            int httpStatus,
            @Nullable String message,
            @Nullable Map<String, List<String>> fieldErrors) {
        this.kind = kind;
        this.httpStatus = httpStatus;
        this.message = message;
        this.fieldErrors = fieldErrors == null
                ? Collections.emptyMap()
                : Collections.unmodifiableMap(fieldErrors);
    }

    public static AppError of(@NonNull Kind kind) {
        return new AppError(kind, 0, null, null);
    }

    @NonNull
    public Kind getKind() {
        return kind;
    }

    /** The HTTP status code, or 0 when there was no HTTP response. */
    public int getHttpStatus() {
        return httpStatus;
    }

    /** The backend's client-facing message (ProblemDetails title), when one was sent. */
    @Nullable
    public String getMessage() {
        return message;
    }

    /** Validation messages keyed by the backend's snake_case request field name. */
    @NonNull
    public Map<String, List<String>> getFieldErrors() {
        return fieldErrors;
    }

    @NonNull
    @Override
    public String toString() {
        return "AppError{" + kind + ", status=" + httpStatus + '}';
    }
}
