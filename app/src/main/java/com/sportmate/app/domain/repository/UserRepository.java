package com.sportmate.app.domain.repository;

import com.sportmate.app.domain.common.Cancellable;
import com.sportmate.app.domain.common.ResultCallback;
import com.sportmate.app.domain.model.User;

public interface UserRepository {
    Cancellable getCurrentUser(ResultCallback<User> callback);
}
