package com.sportmate.app.data.remote.dto;

import com.google.gson.annotations.SerializedName;

/** Backend {@code UserResponse}. */
public final class UserResponseDto {
    @SerializedName("id")
    public String id;
    @SerializedName("email")
    public String email;
    @SerializedName("display_name")
    public String displayName;
    @SerializedName("phone")
    public String phone;
    /** {@code "active"} or {@code "suspended"}. */
    @SerializedName("status")
    public String status;
    /** {@code "User"}, {@code "CourtOwner"} or {@code "Admin"}. */
    @SerializedName("role")
    public String role;
}
