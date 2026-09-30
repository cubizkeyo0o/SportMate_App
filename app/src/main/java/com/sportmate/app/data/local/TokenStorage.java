package com.sportmate.app.data.local;

import androidx.annotation.Nullable;

/**
 * Stores the JWT access token and the refresh token of the signed-in user. Implementations must
 * be thread-safe (the auth interceptor reads from OkHttp threads) and must never log tokens.
 */
public interface TokenStorage {
    @Nullable
    String getAccessToken();

    @Nullable
    String getRefreshToken();

    void saveTokens(String accessToken, String refreshToken);

    void clear();
}
