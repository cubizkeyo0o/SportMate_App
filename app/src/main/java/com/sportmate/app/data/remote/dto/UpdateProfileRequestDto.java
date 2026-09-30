package com.sportmate.app.data.remote.dto;

import com.google.gson.annotations.SerializedName;

/**
 * Backend {@code UpdateProfileRequest}: a full replacement of the editable profile fields. A null
 * phone is omitted from the JSON, which the backend treats as clearing it.
 */
public final class UpdateProfileRequestDto {
    @SerializedName("display_name")
    public final String displayName;
    @SerializedName("phone")
    public final String phone;

    public UpdateProfileRequestDto(String displayName, String phone) {
        this.displayName = displayName;
        this.phone = phone;
    }
}
