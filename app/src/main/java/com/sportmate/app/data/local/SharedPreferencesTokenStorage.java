package com.sportmate.app.data.local;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.Nullable;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.hilt.android.qualifiers.ApplicationContext;

/**
 * App-private SharedPreferences. The manifest disables backup and device-to-device transfer
 * (allowBackup, backup_rules.xml, data_extraction_rules.xml) so tokens are not copied off the
 * device. Values are not encrypted at rest.
 */
@Singleton
public final class SharedPreferencesTokenStorage implements TokenStorage {
    private static final String PREFS_NAME = "sportmate_auth";
    private static final String KEY_ACCESS_TOKEN = "access_token";
    private static final String KEY_REFRESH_TOKEN = "refresh_token";

    private final SharedPreferences preferences;

    @Inject
    public SharedPreferencesTokenStorage(@ApplicationContext Context context) {
        preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    @Nullable
    @Override
    public String getAccessToken() {
        return preferences.getString(KEY_ACCESS_TOKEN, null);
    }

    @Nullable
    @Override
    public String getRefreshToken() {
        return preferences.getString(KEY_REFRESH_TOKEN, null);
    }

    @Override
    public void saveTokens(String accessToken, String refreshToken) {
        preferences.edit()
                .putString(KEY_ACCESS_TOKEN, accessToken)
                .putString(KEY_REFRESH_TOKEN, refreshToken)
                .apply();
    }

    @Override
    public void clear() {
        preferences.edit().clear().apply();
    }
}
