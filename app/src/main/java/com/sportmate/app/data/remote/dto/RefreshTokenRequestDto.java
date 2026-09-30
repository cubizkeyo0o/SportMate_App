package com.sportmate.app.data.remote.dto;

import com.google.gson.annotations.SerializedName;

/** Backend {@code RefreshTokenRequest}, used by refresh and logout. */
public final class RefreshTokenRequestDto {
    @SerializedName("refresh_token")
    public final String refreshToken;

    public RefreshTokenRequestDto(String refreshToken) {
        this.refreshToken = refreshToken;
    }
}
