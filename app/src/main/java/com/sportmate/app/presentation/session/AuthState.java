package com.sportmate.app.presentation.session;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.sportmate.app.domain.common.AppError;
import com.sportmate.app.domain.model.User;

/** Whether the app has a signed-in user. Held by {@link SessionManager}. */
public final class AuthState {
    public enum Status {
        /** The stored session is being checked with the backend. */
        LOADING,
        UNAUTHENTICATED,
        AUTHENTICATED,
        /** The stored session could not be checked (network or server error); it may be retried. */
        ERROR
    }

    private final Status status;
    @Nullable
    private final User user;
    @Nullable
    private final AppError error;

    private AuthState(@NonNull Status status, @Nullable User user, @Nullable AppError error) {
        this.status = status;
        this.user = user;
        this.error = error;
    }

    static AuthState loading() {
        return new AuthState(Status.LOADING, null, null);
    }

    static AuthState unauthenticated() {
        return new AuthState(Status.UNAUTHENTICATED, null, null);
    }

    static AuthState authenticated(@NonNull User user) {
        return new AuthState(Status.AUTHENTICATED, user, null);
    }

    static AuthState error(@NonNull AppError error) {
        return new AuthState(Status.ERROR, null, error);
    }

    @NonNull
    public Status getStatus() {
        return status;
    }

    /** The signed-in user; non-null only when {@link Status#AUTHENTICATED}. */
    @Nullable
    public User getUser() {
        return user;
    }

    /** Non-null only when {@link Status#ERROR}. */
    @Nullable
    public AppError getError() {
        return error;
    }
}
