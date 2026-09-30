package com.sportmate.app.data.remote.dto;

import com.google.gson.annotations.SerializedName;

/** Backend {@code RegisterRequest}. */
public final class RegisterRequestDto {
    @SerializedName("email")
    public final String email;
    @SerializedName("password")
    public final String password;
    @SerializedName("display_name")
    public final String displayName;
    @SerializedName("phone")
    public final String phone;

    public RegisterRequestDto(String email, String password, String displayName, String phone) {
        this.email = email;
        this.password = password;
        this.displayName = displayName;
        this.phone = phone;
    }
}
