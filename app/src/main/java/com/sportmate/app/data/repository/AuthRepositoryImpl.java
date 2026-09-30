package com.sportmate.app.data.repository;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.sportmate.app.data.local.TokenStorage;
import com.sportmate.app.data.remote.ApiCallExecutor;
import com.sportmate.app.data.remote.api.AuthApi;
import com.sportmate.app.data.remote.api.UserApi;
import com.sportmate.app.data.remote.dto.LoginRequestDto;
import com.sportmate.app.data.remote.dto.RefreshTokenRequestDto;
import com.sportmate.app.data.remote.dto.RegisterRequestDto;
import com.sportmate.app.data.remote.mapper.DtoMapper;
import com.sportmate.app.domain.common.AppError;
import com.sportmate.app.domain.common.Cancellable;
import com.sportmate.app.domain.common.ResultCallback;
import com.sportmate.app.domain.model.User;
import com.sportmate.app.domain.repository.AuthRepository;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public final class AuthRepositoryImpl implements AuthRepository {
    private final AuthApi authApi;
    private final UserApi userApi;
    private final TokenStorage tokenStorage;
    private final ApiCallExecutor executor;

    @Inject
    public AuthRepositoryImpl(
            AuthApi authApi,
            UserApi userApi,
            TokenStorage tokenStorage,
            ApiCallExecutor executor) {
        this.authApi = authApi;
        this.userApi = userApi;
        this.tokenStorage = tokenStorage;
        this.executor = executor;
    }

    @Override
    public boolean hasStoredSession() {
        String accessToken = tokenStorage.getAccessToken();
        return accessToken != null && !accessToken.isEmpty();
    }

    @Override
    public Cancellable restoreSession(ResultCallback<User> callback) {
        return executor.execute(userApi.getMe(), DtoMapper::toUser, new ResultCallback<User>() {
            @Override
            public void onSuccess(User user) {
                callback.onSuccess(user);
            }

            @Override
            public void onError(@NonNull AppError error) {
                if (error.getKind() == AppError.Kind.UNAUTHORIZED) {
                    tokenStorage.clear();
                }
                callback.onError(error);
            }
        });
    }

    @Override
    public Cancellable register(
            String email,
            String password,
            String displayName,
            @Nullable String phone,
            ResultCallback<User> callback) {
        return executor.execute(
                authApi.register(new RegisterRequestDto(email, password, displayName, phone)),
                DtoMapper::toUser,
                callback);
    }

    @Override
    public Cancellable login(String email, String password, ResultCallback<User> callback) {
        return executor.execute(
                authApi.login(new LoginRequestDto(email, password)),
                body -> {
                    // Map first so tokens are only stored for a complete response.
                    User user = DtoMapper.toUser(body.user);
                    if (body.accessToken == null || body.refreshToken == null) {
                        throw new IllegalStateException("tokens are missing");
                    }
                    tokenStorage.saveTokens(body.accessToken, body.refreshToken);
                    return user;
                },
                callback);
    }

    @Override
    public Cancellable logout(ResultCallback<Void> callback) {
        String accessToken = tokenStorage.getAccessToken();
        String refreshToken = tokenStorage.getRefreshToken();
        tokenStorage.clear();

        if (accessToken == null || refreshToken == null) {
            callback.onSuccess(null);
            return () -> { };
        }

        // Tokens are already cleared, so the captured access token is attached explicitly.
        return executor.executeNoContent(
                authApi.logout("Bearer " + accessToken, new RefreshTokenRequestDto(refreshToken)),
                callback);
    }
}
