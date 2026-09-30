package com.sportmate.app.data.remote.dto;

import com.google.gson.annotations.SerializedName;

/** Backend {@code LoginRequest}. */
public final class LoginRequestDto {
    @SerializedName("email")
    public final String email;
    @SerializedName("password")
    public final String password;

    public LoginRequestDto(String email, String password) {
        this.email = email;
        this.password = password;
    }
}
