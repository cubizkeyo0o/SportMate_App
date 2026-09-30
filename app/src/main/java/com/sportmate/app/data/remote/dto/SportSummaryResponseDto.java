package com.sportmate.app.data.remote.dto;

import com.google.gson.annotations.SerializedName;

/** Backend {@code SportSummaryResponse}: a sport referenced by a profile, active or not. */
public final class SportSummaryResponseDto {
    @SerializedName("id")
    public String id;
    @SerializedName("name")
    public String name;
    @SerializedName("slug")
    public String slug;
    @SerializedName("is_active")
    public Boolean isActive;
}
