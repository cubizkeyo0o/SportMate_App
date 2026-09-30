package com.sportmate.app.domain.repository;

import androidx.annotation.Nullable;

import com.sportmate.app.domain.common.Cancellable;
import com.sportmate.app.domain.common.ResultCallback;
import com.sportmate.app.domain.model.User;

/**
 * Sign-in state backed by stored tokens. The only component that reads or writes tokens; callers
 * never see them.
 */
public interface AuthRepository {
    /** Whether an access token is stored. Says nothing about whether the backend still accepts it. */
    boolean hasStoredSession();

    /**
     * Validates the stored access token by loading the current user. A 401 means the token is no
     * longer accepted: stored tokens are cleared and the error is delivered as UNAUTHORIZED. Other
     * errors keep the tokens.
     */
    Cancellable restoreSession(ResultCallback<User> callback);

    /** Creates an account with the User role. The user is not signed in afterwards. */
    Cancellable register(
            String email,
            String password,
            String displayName,
            @Nullable String phone,
            ResultCallback<User> callback);

    /** Signs in and stores the returned tokens. Nothing is stored when sign-in fails. */
    Cancellable login(String email, String password, ResultCallback<User> callback);

    /**
     * Signs out on this device immediately: local tokens are cleared before this method returns.
     * The backend is then asked to revoke the refresh token; the callback only reports that
     * best-effort call, and its failure does not undo the local sign-out.
     */
    Cancellable logout(ResultCallback<Void> callback);
}
