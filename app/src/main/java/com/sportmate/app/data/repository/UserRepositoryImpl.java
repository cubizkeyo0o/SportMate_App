package com.sportmate.app.data.repository;

import com.sportmate.app.data.remote.ApiCallExecutor;
import com.sportmate.app.data.remote.api.UserApi;
import com.sportmate.app.data.remote.mapper.DtoMapper;
import com.sportmate.app.domain.common.Cancellable;
import com.sportmate.app.domain.common.ResultCallback;
import com.sportmate.app.domain.model.User;
import com.sportmate.app.domain.repository.UserRepository;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public final class UserRepositoryImpl implements UserRepository {
    private final UserApi userApi;
    private final ApiCallExecutor executor;

    @Inject
    public UserRepositoryImpl(UserApi userApi, ApiCallExecutor executor) {
        this.userApi = userApi;
        this.executor = executor;
    }

    @Override
    public Cancellable getCurrentUser(ResultCallback<User> callback) {
        return executor.execute(userApi.getMe(), DtoMapper::toUser, callback);
    }
}
