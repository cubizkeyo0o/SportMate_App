package com.sportmate.app.domain.repository;

import androidx.annotation.Nullable;

import com.sportmate.app.domain.common.Cancellable;
import com.sportmate.app.domain.common.ResultCallback;
import com.sportmate.app.domain.model.User;

/** The authenticated user's own account. */
public interface UserRepository {
    Cancellable getCurrentUser(ResultCallback<User> callback);

    /**
     * Replaces the editable profile fields and delivers the saved user. A null phone clears it.
     * Email, role and status are not editable.
     */
    Cancellable updateProfile(String displayName, @Nullable String phone, ResultCallback<User> callback);
}
