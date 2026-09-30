package com.sportmate.app.presentation.common;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.sportmate.app.domain.common.AppError;

/** Loading / success / error state that a ViewModel exposes through LiveData. */
public final class UiState<T> {
    public enum Status {
        LOADING,
        SUCCESS,
        ERROR
    }

    private final Status status;
    @Nullable
    private final T data;
    @Nullable
    private final AppError error;

    private UiState(@NonNull Status status, @Nullable T data, @Nullable AppError error) {
        this.status = status;
        this.data = data;
        this.error = error;
    }

    public static <T> UiState<T> loading() {
        return new UiState<>(Status.LOADING, null, null);
    }

    public static <T> UiState<T> success(@Nullable T data) {
        return new UiState<>(Status.SUCCESS, data, null);
    }

    public static <T> UiState<T> error(@NonNull AppError error) {
        return new UiState<>(Status.ERROR, null, error);
    }

    @NonNull
    public Status getStatus() {
        return status;
    }

    @Nullable
    public T getData() {
        return data;
    }

    @Nullable
    public AppError getError() {
        return error;
    }
}
