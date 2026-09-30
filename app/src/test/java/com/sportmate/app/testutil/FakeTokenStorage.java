package com.sportmate.app.testutil;

import com.sportmate.app.data.local.TokenStorage;

public final class FakeTokenStorage implements TokenStorage {
    private volatile String accessToken;
    private volatile String refreshToken;

    public FakeTokenStorage() {
    }

    public FakeTokenStorage(String accessToken, String refreshToken) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
    }

    @Override
    public String getAccessToken() {
        return accessToken;
    }

    @Override
    public String getRefreshToken() {
        return refreshToken;
    }

    @Override
    public void saveTokens(String accessToken, String refreshToken) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
    }

    @Override
    public void clear() {
        accessToken = null;
        refreshToken = null;
    }
}
