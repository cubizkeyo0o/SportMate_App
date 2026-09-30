package com.sportmate.app.data.remote.dto;

import com.google.gson.annotations.SerializedName;

/** Backend {@code AuthResponse}. Never log instances of this class: it carries tokens. */
public final class AuthResponseDto {
    @SerializedName("access_token")
    public String accessToken;
    @SerializedName("refresh_token")
    public String refreshToken;
    @SerializedName("token_type")
    public String tokenType;
    /** Access token lifetime in seconds. */
    @SerializedName("expires_in")
    public int expiresIn;
    @SerializedName("user")
    public UserResponseDto user;
}
