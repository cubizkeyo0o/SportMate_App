package com.sportmate.app.domain.repository;

import androidx.annotation.Nullable;

import com.sportmate.app.domain.common.Cancellable;
import com.sportmate.app.domain.common.ResultCallback;
import com.sportmate.app.domain.model.User;

public interface AuthRepository {
    /** Whether tokens from a previous sign-in are stored. Does not check them with the backend. */
    boolean hasSession();

    /** Creates an account. The user is not signed in afterwards. */
    Cancellable register(
            String email,
            String password,
            String displayName,
            @Nullable String phone,
            ResultCallback<User> callback);

    /** Signs in and stores the returned tokens. */
    Cancellable login(String email, String password, ResultCallback<User> callback);

    /**
     * Revokes the refresh token on the backend and clears local tokens. Local tokens are cleared
     * even when the backend call fails, so the user is always signed out on this device. Cancelling
     * before the backend answers keeps the local tokens.
     */
    Cancellable logout(ResultCallback<Void> callback);
}
