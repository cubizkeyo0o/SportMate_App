package com.sportmate.app.testutil;

import androidx.annotation.Nullable;

import com.sportmate.app.domain.common.Cancellable;
import com.sportmate.app.domain.common.ResultCallback;
import com.sportmate.app.domain.model.User;
import com.sportmate.app.domain.model.UserRole;
import com.sportmate.app.domain.model.UserStatus;
import com.sportmate.app.domain.repository.AuthRepository;

/**
 * Records calls and keeps each callback so a test can complete it with a result or AppError. Used
 * for state-transition tests; HTTP behaviour is covered against MockWebServer.
 */
public final class ControllableAuthRepository implements AuthRepository {
    public boolean storedSession;
    public int restoreCalls;
    public int loginCalls;
    public int registerCalls;
    public int logoutCalls;
    public int cancelled;

    public String lastEmail;
    public String lastPassword;
    public String lastDisplayName;
    public String lastPhone;

    public ResultCallback<User> restoreCallback;
    public ResultCallback<User> loginCallback;
    public ResultCallback<User> registerCallback;

    public static User user() {
        return new User("4f1c2d7e-8a8b-4d8e-9d52-2b1f0c1a7e11", "a@example.com", "Alex", null,
                UserStatus.ACTIVE, UserRole.USER);
    }

    @Override
    public boolean hasStoredSession() {
        return storedSession;
    }

    @Override
    public Cancellable restoreSession(ResultCallback<User> callback) {
        restoreCalls++;
        restoreCallback = callback;
        return this::cancel;
    }

    @Override
    public Cancellable register(
            String email,
            String password,
            String displayName,
            @Nullable String phone,
            ResultCallback<User> callback) {
        registerCalls++;
        lastEmail = email;
        lastPassword = password;
        lastDisplayName = displayName;
        lastPhone = phone;
        registerCallback = callback;
        return this::cancel;
    }

    @Override
    public Cancellable login(String email, String password, ResultCallback<User> callback) {
        loginCalls++;
        lastEmail = email;
        lastPassword = password;
        loginCallback = callback;
        return this::cancel;
    }

    // Mirrors the real contract: local sign-out is immediate and the callback is best-effort.
    @Override
    public Cancellable logout(ResultCallback<Void> callback) {
        logoutCalls++;
        storedSession = false;
        callback.onSuccess(null);
        return () -> { };
    }

    private void cancel() {
        cancelled++;
    }
}
